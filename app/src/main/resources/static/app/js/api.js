// cliente da API REST /api/v1 com o token JWT guardado no aparelho

const BASE = '/api/v1';
const CHAVE_SESSAO = 'jcondo.sessao';
const CHAVE_FILA = 'jcondo.fila';

export class ErroApi extends Error {
    constructor(status, mensagem, campos = null, offline = false) {
        super(mensagem);
        this.status = status;
        this.campos = campos;
        this.offline = offline;
    }
}

// ---------- sessao ----------
export function sessao() {
    try {
        const s = JSON.parse(localStorage.getItem(CHAVE_SESSAO));
        if (!s || new Date(s.expiraEm) <= new Date()) {
            return null;
        }
        return s;
    } catch {
        return null;
    }
}

export function usuario() {
    return sessao()?.usuario ?? null;
}

function salvarSessao(dados) {
    localStorage.setItem(CHAVE_SESSAO, JSON.stringify({
        token: dados.token,
        expiraEm: dados.expiraEm,
        usuario: dados.usuario
    }));
}

export async function entrar(email, senha) {
    const dados = await api('/auth/login', { metodo: 'POST', corpo: { email, senha } });
    salvarSessao(dados);
    return dados.usuario;
}

export function sair() {
    localStorage.removeItem(CHAVE_SESSAO);
    localStorage.removeItem(CHAVE_FILA);
    // apaga os dados da API guardados offline (eram do usuario que saiu)
    navigator.serviceWorker?.controller?.postMessage({ tipo: 'limpar-api' });
}

// ---------- requisicoes ----------
export async function api(caminho, { metodo = 'GET', corpo } = {}) {
    const headers = { Accept: 'application/json' };
    if (corpo !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    const s = sessao();
    if (s) {
        headers.Authorization = `Bearer ${s.token}`;
    }

    let resposta;
    try {
        resposta = await fetch(BASE + caminho, {
            method: metodo,
            headers,
            body: corpo !== undefined ? JSON.stringify(corpo) : undefined
        });
    } catch {
        throw new ErroApi(0, 'Sem conexão com o servidor.', null, true);
    }

    // o service worker marca quando a resposta veio do cache por falta de internet
    const doCache = resposta.headers.get('X-JCondo-Offline') === '1';
    window.dispatchEvent(new CustomEvent('jcondo:origem-dados', { detail: { doCache } }));

    if (resposta.status === 401 && caminho !== '/auth/login') {
        sair();
        location.hash = '#/login';
        throw new ErroApi(401, 'Sua sessão expirou. Entre novamente.');
    }
    if (resposta.status === 204) {
        return null;
    }

    let dados = null;
    try {
        dados = await resposta.json();
    } catch {
        dados = null;
    }

    if (!resposta.ok) {
        const offline = resposta.status === 503 && dados?.offline;
        throw new ErroApi(resposta.status, dados?.detail || 'Não foi possível concluir a operação.',
            dados?.campos, offline);
    }
    return dados;
}

// ---------- fila offline ----------
// ocorrencia aberta sem internet fica guardada e e enviada quando a conexao volta
export function fila() {
    try {
        return JSON.parse(localStorage.getItem(CHAVE_FILA)) || [];
    } catch {
        return [];
    }
}

export function enfileirar(ocorrencia) {
    const itens = fila();
    itens.push({ ...ocorrencia, idLocal: Date.now(), criadaEm: new Date().toISOString() });
    localStorage.setItem(CHAVE_FILA, JSON.stringify(itens));
}

let enviando = false;

export async function enviarFila() {
    if (enviando || !navigator.onLine || !sessao()) return 0;
    enviando = true;
    let enviadas = 0;
    try {
        for (const item of fila()) {
            try {
                await api('/ocorrencias', {
                    metodo: 'POST',
                    corpo: { titulo: item.titulo, descricao: item.descricao, categoria: item.categoria }
                });
                enviadas++;
            } catch (e) {
                if (e.offline) break; // ainda sem internet, tenta depois
                // erro de validacao: descarta pra nao travar a fila
            }
            const restantes = fila().filter(i => i.idLocal !== item.idLocal);
            localStorage.setItem(CHAVE_FILA, JSON.stringify(restantes));
        }
    } finally {
        enviando = false;
    }
    return enviadas;
}
