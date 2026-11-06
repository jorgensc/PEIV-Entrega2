// ponto de entrada do PWA: rotas, casca (abas/menu), service worker e modo offline
import { enviarFila, fila, sessao } from './api.js';
import {
    telaAviso, telaAvisos, telaInicio, telaLogin, telaNovaOcorrencia, telaNovaReserva, telaNovoAviso,
    telaOcorrencia, telaOcorrencias, telaPerfil, telaReservas
} from './telas.js';
import { ic, toast } from './util.js';

const ROTAS = [
    { re: /^#\/login$/, tela: telaLogin, publica: true },
    { re: /^#\/inicio$/, tela: telaInicio, aba: 'inicio', titulo: 'Início' },
    { re: /^#\/avisos$/, tela: telaAvisos, aba: 'avisos', titulo: 'Avisos' },
    { re: /^#\/avisos\/novo$/, tela: telaNovoAviso, aba: 'avisos', titulo: 'Novo aviso', voltar: '#/avisos' },
    { re: /^#\/avisos\/(\d+)$/, tela: telaAviso, aba: 'avisos', titulo: 'Aviso', voltar: '#/avisos' },
    { re: /^#\/reservas$/, tela: telaReservas, aba: 'reservas', titulo: 'Reservas' },
    { re: /^#\/reservas\/nova$/, tela: telaNovaReserva, aba: 'reservas', titulo: 'Nova reserva', voltar: '#/reservas' },
    { re: /^#\/ocorrencias$/, tela: telaOcorrencias, aba: 'ocorrencias', titulo: 'Ocorrências' },
    { re: /^#\/ocorrencias\/nova$/, tela: telaNovaOcorrencia, aba: 'ocorrencias', titulo: 'Nova ocorrência', voltar: '#/ocorrencias' },
    { re: /^#\/ocorrencias\/(\d+)$/, tela: telaOcorrencia, aba: 'ocorrencias', titulo: 'Ocorrência', voltar: '#/ocorrencias' },
    { re: /^#\/perfil$/, tela: telaPerfil, aba: 'perfil', titulo: 'Perfil' }
];

const ABAS = [
    ['inicio', 'Início'], ['avisos', 'Avisos'], ['reservas', 'Reservas'],
    ['ocorrencias', 'Ocorrências'], ['perfil', 'Perfil']
];

const app = document.getElementById('app');

function montarCasca() {
    app.innerHTML = `
        <div class="casca">
            <nav class="lateral" aria-label="Menu">
                <div class="marca"><img src="icons/icon-192.png" alt="">JCondo</div>
                ${ABAS.map(([id, nome]) => `<a href="#/${id}" data-aba="${id}">${ic(id)}${nome}</a>`).join('')}
                <div class="rodape-lateral">App do morador · v2.0</div>
            </nav>
            <div class="area-principal">
                <header class="topo">
                    <a class="btn-icone oculto" id="voltar" href="#/inicio" aria-label="Voltar">${ic('voltar')}</a>
                    <span class="marca so-celular" id="marcaTopo"><img src="icons/icon-192.png" alt=""></span>
                    <h1 id="titulo">JCondo</h1>
                    <span class="selo-offline oculto" id="seloOffline" role="status">${ic('offline')}<span>Offline</span></span>
                </header>
                <main class="conteudo" id="conteudo" tabindex="-1"></main>
            </div>
            <nav class="abas" aria-label="Navegação principal">
                ${ABAS.map(([id, nome]) => `<a href="#/${id}" data-aba="${id}">${ic(id)}<span>${nome}</span></a>`).join('')}
            </nav>
        </div>`;
    atualizarSelo();
    atualizarContador();
}

async function rotear() {
    const hash = location.hash || '#/inicio';
    const logado = sessao() !== null;

    if (!logado && hash !== '#/login') {
        location.replace('#/login');
        return;
    }
    if (logado && hash === '#/login') {
        location.replace('#/inicio');
        return;
    }
    const rota = ROTAS.find(r => r.re.test(hash));
    if (!rota) {
        location.replace('#/inicio');
        return;
    }
    const [, id] = hash.match(rota.re);
    document.title = rota.titulo ? `${rota.titulo} · JCondo` : 'JCondo';

    if (rota.publica) {
        rota.tela(app, { id });
        return;
    }
    if (!app.querySelector('.casca')) {
        montarCasca();
    }

    app.querySelectorAll('[data-aba]').forEach(a => {
        const ativa = a.dataset.aba === rota.aba;
        a.classList.toggle('ativa', ativa);
        if (ativa) a.setAttribute('aria-current', 'page');
        else a.removeAttribute('aria-current');
    });
    app.querySelector('#titulo').textContent = rota.titulo;
    const voltar = app.querySelector('#voltar');
    voltar.classList.toggle('oculto', !rota.voltar);
    voltar.href = rota.voltar || '#/inicio';
    app.querySelector('#marcaTopo').classList.toggle('oculto', !!rota.voltar);

    const conteudo = app.querySelector('#conteudo');
    window.scrollTo(0, 0);
    await rota.tela(conteudo, { id });
}

// ---------- offline ----------
let dadosDoCache = false;

function atualizarSelo() {
    const selo = document.getElementById('seloOffline');
    if (!selo) return;
    const offline = !navigator.onLine || dadosDoCache;
    selo.classList.toggle('oculto', !offline);
    selo.querySelector('span').textContent = navigator.onLine ? 'Dados salvos' : 'Offline';
    selo.title = 'Sem conexão: mostrando os últimos dados salvos no aparelho';
}

function atualizarContador() {
    const aba = document.querySelector('.abas [data-aba="ocorrencias"]');
    if (!aba) return;
    aba.querySelector('.contador')?.remove();
    const n = fila().length;
    if (n) aba.insertAdjacentHTML('beforeend', `<span class="contador" aria-label="${n} na fila">${n}</span>`);
}

window.addEventListener('jcondo:origem-dados', e => {
    dadosDoCache = e.detail.doCache;
    atualizarSelo();
});
window.addEventListener('jcondo:fila', atualizarContador);

window.addEventListener('offline', () => {
    atualizarSelo();
    toast('Você está offline. O app continua mostrando os dados salvos.');
});

window.addEventListener('online', async () => {
    dadosDoCache = false;
    atualizarSelo();
    const enviadas = await enviarFila();
    atualizarContador();
    if (enviadas) {
        toast(`Conexão de volta: ${enviadas} ocorrência(s) enviada(s) ao síndico.`);
        if (location.hash.startsWith('#/ocorrencias')) rotear();
    } else {
        toast('Conexão restabelecida.');
    }
});

// ---------- service worker ----------
if ('serviceWorker' in navigator) {
    const tinhaControle = Boolean(navigator.serviceWorker.controller);
    let recarregando = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        // so recarrega quando e uma atualizacao (na primeira instalacao nao precisa)
        if (!tinhaControle || recarregando) return;
        recarregando = true;
        location.reload();
    });

    window.addEventListener('load', async () => {
        try {
            const reg = await navigator.serviceWorker.register('sw.js', { scope: './' });
            reg.addEventListener('updatefound', () => {
                const novo = reg.installing;
                novo?.addEventListener('statechange', () => {
                    if (novo.state === 'installed' && navigator.serviceWorker.controller) {
                        toast('Nova versão do JCondo disponível.', {
                            texto: 'Atualizar',
                            aoClicar: () => novo.postMessage({ tipo: 'pular-espera' })
                        });
                    }
                });
            });
        } catch (e) {
            console.warn('Service worker nao registrado', e);
        }
    });
}

window.addEventListener('hashchange', rotear);
rotear();
enviarFila().then(atualizarContador);
