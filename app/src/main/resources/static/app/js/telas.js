// telas do app. Cada tela recebe o elemento onde desenhar e os parametros da rota
import { api, entrar, enfileirar, ehSindico, fila, sair, usuario } from './api.js';
import {
    blocoData, carregando, dataHoraCurta, dataPorExtenso, diaSemana, esc, hojeIso, hora, ic,
    iniciais, isoDia, mensagemErro, minutos, mostrarErrosCampos, paraData, primeiroNome, relativo,
    somarDias, toast, vazio
} from './util.js';
import { instalacao } from './instalar.js';

const CATEGORIAS_OCORRENCIA = [
    ['MANUTENCAO', 'Manutenção', 'Vazamento, lâmpada, portão'],
    ['BARULHO', 'Barulho', 'Som alto, obras fora de hora'],
    ['SEGURANCA', 'Segurança', 'Acesso, câmeras, portaria'],
    ['LIMPEZA', 'Limpeza', 'Áreas comuns, lixo'],
    ['OUTROS', 'Outros', 'Qualquer outro assunto']
];

const CATEGORIAS_AVISO = [
    ['GERAL', 'Geral'], ['MANUTENCAO', 'Manutenção'], ['ASSEMBLEIA', 'Assembleia'],
    ['SEGURANCA', 'Segurança'], ['EVENTO', 'Evento']
];

function ir(rota) {
    location.hash = rota;
}

function situacaoReserva(r) {
    if (r.status === 'CANCELADA') return ['CANCELADA', 'Cancelada'];
    if (r.data < hojeIso()) return ['REALIZADA', 'Realizada'];
    return ['CONFIRMADA', 'Confirmada'];
}

function itemReserva(r, comMorador) {
    const [cls, texto] = situacaoReserva(r);
    return `
        <li class="item">
            ${blocoData(r.data)}
            <div class="meio">
                <div class="titulo">${esc(r.area)}</div>
                <div class="sub">${esc(diaSemana(paraData(r.data)))}, ${esc(r.horaInicio)} às ${esc(r.horaFim)}
                    ${comMorador ? ` · ${esc(r.morador)} (${esc(r.apartamento)} ${esc(r.bloco)})` : ''}</div>
            </div>
            <span class="status status-${cls}">${texto}</span>
        </li>`;
}

function cartaoAviso(a) {
    return `
        <a class="cartao aviso ${a.importante ? 'importante' : ''}" href="#/avisos/${a.id}">
            <div class="linha-topo">
                <span class="chip">${esc(a.categoriaDescricao)}</span>
                ${a.importante ? `<span class="status status-ABERTA">${ic('fixo')}Importante</span>` : ''}
                <span class="quando">${esc(relativo(a.publicadoEm))}</span>
            </div>
            <h4>${esc(a.titulo)}</h4>
            <p>${esc(a.conteudo)}</p>
        </a>`;
}

// ======================================================= LOGIN
export function telaLogin(el) {
    el.innerHTML = `
        <div class="login">
            <div class="login-topo">
                <img src="icons/icon-192.png" alt="">
                <h1>JCondo</h1>
                <p>Avisos, reservas e ocorrências do seu condomínio na palma da mão.</p>
            </div>
            <div class="login-form">
                <div class="caixa">
                    <h2 style="margin:0 0 4px">Entrar</h2>
                    <p class="pequeno" style="margin:0 0 20px">Use o e-mail cadastrado pelo síndico.</p>
                    <div id="msg"></div>
                    <form id="formLogin" novalidate>
                        <div class="campo">
                            <label for="email">E-mail</label>
                            <input class="entrada" type="email" id="email" name="email" autocomplete="username"
                                   inputmode="email" placeholder="voce@exemplo.com" required>
                        </div>
                        <div class="campo">
                            <label for="senha">Senha</label>
                            <input class="entrada" type="password" id="senha" name="senha"
                                   autocomplete="current-password" placeholder="Sua senha" required>
                        </div>
                        <button class="botao primario bloco" type="submit">Entrar</button>
                    </form>
                    <div class="demo">
                        <strong>${ic('info')} Acesso de demonstração</strong>
                        <div class="pequeno">Moradores: senha <b>morador123</b>. Síndico: <b>sindico123</b>.</div>
                        <div class="botoes">
                            <button class="botao" type="button" data-email="ana.souza@email.com" data-senha="morador123">Morador (Ana)</button>
                            <button class="botao" type="button" data-email="sindico@jcondo.com" data-senha="sindico123">Síndico</button>
                        </div>
                    </div>
                    <p class="pequeno" style="text-align:center;margin-top:20px">
                        É síndico? <a href="/login">Abrir o painel web</a>
                    </p>
                </div>
            </div>
        </div>`;

    const form = el.querySelector('#formLogin');
    el.querySelectorAll('[data-email]').forEach(b => b.addEventListener('click', () => {
        form.email.value = b.dataset.email;
        form.senha.value = b.dataset.senha;
        form.requestSubmit();
    }));

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const botao = form.querySelector('button[type=submit]');
        botao.disabled = true;
        botao.textContent = 'Entrando...';
        try {
            const u = await entrar(form.email.value.trim(), form.senha.value);
            toast(`Bem-vindo(a), ${primeiroNome(u.nome)}!`);
            ir('#/inicio');
        } catch (erro) {
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(form, erro.campos);
            botao.disabled = false;
            botao.textContent = 'Entrar';
        }
    });
}

// ======================================================= INICIO
export async function telaInicio(el) {
    el.innerHTML = carregando();
    const u = usuario();
    let r;
    try {
        r = await api('/resumo');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    const sindico = u.perfil === 'SINDICO';
    const pendentes = fila().length;

    el.innerHTML = `
        <div class="saudacao">
            <h2>Olá, ${esc(primeiroNome(u.nome))}</h2>
            <p>${sindico ? 'Síndico do condomínio' : `Apartamento ${esc(u.morador?.apartamento)} · Bloco ${esc(u.morador?.bloco)}`}</p>
        </div>

        <div id="bannerInstalar"></div>

        ${pendentes ? `<div class="mensagem alerta">${ic('offline')}<span>${pendentes} ocorrência(s) aguardando internet para serem enviadas.</span></div>` : ''}

        <div class="numeros">
            ${sindico ? `
            <a class="cartao numero" href="#/reservas">
                <span class="icone-bola cor-azul">${ic('pessoas')}</span>
                <strong>${r.moradores}</strong><span>Moradores</span>
            </a>` : ''}
            <a class="cartao numero" href="#/reservas">
                <span class="icone-bola cor-verde">${ic('reservas')}</span>
                <strong>${r.reservasFuturas}</strong><span>${sindico ? 'Reservas futuras' : 'Minhas reservas'}</span>
            </a>
            <a class="cartao numero" href="#/ocorrencias">
                <span class="icone-bola cor-ambar">${ic('ocorrencias')}</span>
                <strong>${r.ocorrenciasEmAberto}</strong><span>Ocorrências em aberto</span>
            </a>
            ${sindico ? `
            <a class="cartao numero" href="#/avisos">
                <span class="icone-bola cor-azul">${ic('avisos')}</span>
                <strong>${r.avisos}</strong><span>Avisos</span>
            </a>` : ''}
        </div>

        <div class="secao-titulo"><h3>Acesso rápido</h3></div>
        <div class="atalhos">
            ${sindico ? `
            <a class="cartao atalho" href="#/avisos/novo"><span class="icone-bola cor-azul">${ic('avisos')}</span>Publicar aviso</a>
            <a class="cartao atalho" href="#/ocorrencias"><span class="icone-bola cor-ambar">${ic('ocorrencias')}</span>Atender ocorrências</a>
            ` : `
            <a class="cartao atalho" href="#/reservas/nova"><span class="icone-bola cor-verde">${ic('reservas')}</span>Reservar área</a>
            <a class="cartao atalho" href="#/ocorrencias/nova"><span class="icone-bola cor-ambar">${ic('ocorrencias')}</span>Abrir ocorrência</a>
            `}
        </div>

        <div class="secao-titulo"><h3>${sindico ? 'Próximas reservas' : 'Minhas próximas reservas'}</h3><a href="#/reservas">Ver todas</a></div>
        <div class="cartao">
            ${r.proximasReservas.length
                ? `<ul class="lista">${r.proximasReservas.map(x => itemReserva(x, sindico)).join('')}</ul>`
                : vazio('reservas', sindico ? 'Nenhuma reserva marcada.' : 'Você não tem reservas marcadas.')}
        </div>

        <div class="secao-titulo"><h3>Avisos recentes</h3><a href="#/avisos">Ver mural</a></div>
        ${r.ultimosAvisos.length ? r.ultimosAvisos.map(cartaoAviso).join('') : `<div class="cartao">${vazio('avisos', 'Nenhum aviso publicado.')}</div>`}
    `;

    instalacao.desenharBanner(el.querySelector('#bannerInstalar'));
}

// ======================================================= AVISOS
export async function telaAvisos(el) {
    el.innerHTML = carregando();
    let avisos;
    try {
        avisos = await api('/avisos');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    el.innerHTML = `
        ${avisos.length ? avisos.map(cartaoAviso).join('') : `<div class="cartao">${vazio('avisos', 'Nenhum aviso publicado ainda.')}</div>`}
        ${ehSindico() ? `<a class="fab" href="#/avisos/novo">${ic('mais')}Novo aviso</a>` : ''}
    `;
}

export async function telaAviso(el, { id }) {
    el.innerHTML = carregando();
    let a;
    try {
        a = await api(`/avisos/${id}`);
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    const podeCompartilhar = 'share' in navigator;
    el.innerHTML = `
        <article class="cartao cartao-corpo aviso ${a.importante ? 'importante' : ''}">
            <div class="linha-topo">
                <span class="chip">${esc(a.categoriaDescricao)}</span>
                ${a.importante ? `<span class="status status-ABERTA">Importante</span>` : ''}
            </div>
            <h2 style="margin:12px 0 4px">${esc(a.titulo)}</h2>
            <p class="pequeno" style="margin:0 0 16px">${esc(a.autor)} · ${esc(dataHoraCurta(a.publicadoEm))}</p>
            <div class="texto-longo">${esc(a.conteudo)}</div>
        </article>
        <div style="display:grid;gap:10px;margin-top:14px">
            ${podeCompartilhar ? `<button class="botao bloco" id="compartilhar">${ic('compartilhar')}Compartilhar</button>` : ''}
            ${ehSindico() ? `<button class="botao perigo bloco" id="excluir">${ic('x')}Excluir aviso</button>` : ''}
        </div>`;

    el.querySelector('#compartilhar')?.addEventListener('click', () => {
        navigator.share({ title: a.titulo, text: `${a.titulo}\n\n${a.conteudo}` }).catch(() => {});
    });
    el.querySelector('#excluir')?.addEventListener('click', async () => {
        if (!confirm('Excluir este aviso? Ele some para todos os moradores.')) return;
        try {
            await api(`/avisos/${id}`, { metodo: 'DELETE' });
            toast('Aviso excluído.');
            ir('#/avisos');
        } catch (erro) {
            toast(erro.message);
        }
    });
}

export function telaNovoAviso(el) {
    if (!ehSindico()) {
        el.innerHTML = mensagemErro({ message: 'Somente o síndico publica avisos.' });
        return;
    }
    el.innerHTML = `
        <form class="cartao cartao-corpo" id="formAviso" novalidate>
            <div id="msg"></div>
            <div class="campo">
                <label for="titulo">Título</label>
                <input class="entrada" id="titulo" name="titulo" maxlength="120" placeholder="Ex.: Manutenção da piscina">
            </div>
            <div class="campo">
                <label for="categoria">Categoria</label>
                <select class="entrada" id="categoria" name="categoria">
                    ${CATEGORIAS_AVISO.map(([v, t]) => `<option value="${v}">${t}</option>`).join('')}
                </select>
            </div>
            <div class="campo">
                <label for="conteudo">Mensagem</label>
                <textarea class="entrada" id="conteudo" name="conteudo" maxlength="2000" rows="6"
                          placeholder="Escreva o comunicado"></textarea>
            </div>
            <label class="campo" style="display:flex;gap:10px;align-items:center;font-weight:600">
                <input type="checkbox" name="importante" style="width:20px;height:20px"> Marcar como importante
            </label>
            <button class="botao primario bloco" type="submit">${ic('enviar')}Publicar</button>
        </form>`;

    const form = el.querySelector('#formAviso');
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const botao = form.querySelector('button');
        botao.disabled = true;
        try {
            await api('/avisos', {
                metodo: 'POST',
                corpo: {
                    titulo: form.titulo.value, conteudo: form.conteudo.value,
                    categoria: form.categoria.value, importante: form.importante.checked
                }
            });
            toast('Aviso publicado para todos os moradores.');
            ir('#/avisos');
        } catch (erro) {
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(form, erro.campos);
            botao.disabled = false;
        }
    });
}

// ======================================================= RESERVAS
export async function telaReservas(el) {
    el.innerHTML = carregando();
    let reservas;
    try {
        reservas = await api('/reservas');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    const sindico = ehSindico();
    const hoje = hojeIso();
    const proximas = reservas
        .filter(r => r.status === 'CONFIRMADA' && r.data >= hoje)
        .sort((a, b) => (a.data + a.horaInicio).localeCompare(b.data + b.horaInicio));
    const historico = reservas.filter(r => !proximas.includes(r));
    let aba = 'proximas';

    function desenhar() {
        const lista = aba === 'proximas' ? proximas : historico;
        el.innerHTML = `
            <div class="segmentos" role="tablist">
                <button role="tab" class="${aba === 'proximas' ? 'ativo' : ''}" data-aba="proximas">Próximas (${proximas.length})</button>
                <button role="tab" class="${aba === 'historico' ? 'ativo' : ''}" data-aba="historico">Histórico (${historico.length})</button>
            </div>
            <div class="cartao">
                ${lista.length ? `<ul class="lista">${lista.map(r => `
                    <li class="item">
                        ${blocoData(r.data)}
                        <div class="meio">
                            <div class="titulo">${esc(r.area)}</div>
                            <div class="sub">${esc(diaSemana(paraData(r.data)))}, ${esc(r.horaInicio)} às ${esc(r.horaFim)}</div>
                            ${sindico ? `<div class="sub">${esc(r.morador)} · ${esc(r.apartamento)} ${esc(r.bloco)}</div>` : ''}
                            ${r.observacao ? `<div class="sub">${esc(r.observacao)}</div>` : ''}
                        </div>
                        ${aba === 'proximas'
                            ? `<button class="botao perigo" style="min-height:38px;padding:0 12px" data-cancelar="${r.id}" aria-label="Cancelar reserva">${ic('x')}</button>`
                            : `<span class="status status-${situacaoReserva(r)[0]}">${situacaoReserva(r)[1]}</span>`}
                    </li>`).join('')}</ul>`
                : vazio('reservas', aba === 'proximas' ? 'Nenhuma reserva marcada.' : 'Nada no histórico ainda.')}
            </div>
            ${sindico ? '' : `<a class="fab" href="#/reservas/nova">${ic('mais')}Reservar</a>`}
        `;
        el.querySelectorAll('[data-aba]').forEach(b => b.addEventListener('click', () => {
            aba = b.dataset.aba;
            desenhar();
        }));
        el.querySelectorAll('[data-cancelar]').forEach(b => b.addEventListener('click', async () => {
            if (!confirm('Cancelar esta reserva? O horário fica livre para outros moradores.')) return;
            try {
                await api(`/reservas/${b.dataset.cancelar}/cancelar`, { metodo: 'PATCH' });
                toast('Reserva cancelada.');
                telaReservas(el);
            } catch (erro) {
                toast(erro.message);
            }
        }));
    }
    desenhar();
}

export async function telaNovaReserva(el) {
    if (ehSindico()) {
        el.innerHTML = `<div class="mensagem info">${ic('info')}<span>O síndico reserva em nome de um morador pelo
            <a href="/reservas/nova">painel web</a>. No app, a reserva é feita pelo próprio morador.</span></div>`;
        return;
    }
    el.innerHTML = carregando();
    let areas;
    try {
        areas = await api('/areas');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    if (!areas.length) {
        el.innerHTML = vazio('predio', 'Nenhuma área disponível para reserva.');
        return;
    }

    const estado = { areaId: areas[0].id, data: isoDia(somarDias(new Date(), 1)), inicio: null, fim: null };
    const dias = Array.from({ length: 30 }, (_, i) => somarDias(new Date(), i));

    el.innerHTML = `
        <form id="formReserva" novalidate>
            <div id="msg"></div>
            <span class="rotulo">1. Escolha a área</span>
            <div class="opcoes" style="margin-bottom:18px">
                ${areas.map((a, i) => `
                    <label class="opcao">
                        <input type="radio" name="area" value="${a.id}" ${i === 0 ? 'checked' : ''}>
                        <span><strong>${esc(a.nome)}</strong>
                            <small>${esc(a.horaAbertura)} às ${esc(a.horaFechamento)} · até ${a.capacidade} pessoas</small></span>
                    </label>`).join('')}
            </div>

            <span class="rotulo">2. Escolha o dia</span>
            <div class="dias" id="dias">
                ${dias.map(d => `
                    <button type="button" class="dia" data-dia="${isoDia(d)}">
                        <small>${esc(diaSemana(d))}</small><strong>${d.getDate()}</strong>
                    </button>`).join('')}
            </div>
            <p class="pequeno" id="diaExtenso" style="margin:2px 0 16px"></p>

            <span class="rotulo">3. Escolha o horário</span>
            <div class="cartao cartao-corpo" id="horarios" style="margin-bottom:16px">${carregando()}</div>

            <div class="campo">
                <label for="observacao">Observação (opcional)</label>
                <input class="entrada" id="observacao" name="observacao" maxlength="255" placeholder="Ex.: aniversário, 20 convidados">
            </div>
            <button class="botao primario bloco" type="submit" id="confirmar" disabled>${ic('check')}Confirmar reserva</button>
        </form>`;

    const form = el.querySelector('#formReserva');
    const caixaHorarios = el.querySelector('#horarios');

    function marcarDia() {
        el.querySelectorAll('.dia').forEach(b => {
            const ativo = b.dataset.dia === estado.data;
            b.classList.toggle('ativo', ativo);
            b.setAttribute('aria-pressed', ativo);
        });
        el.querySelector('#diaExtenso').textContent = dataPorExtenso(estado.data);
    }

    async function carregarHorarios() {
        estado.inicio = null;
        estado.fim = null;
        form.querySelector('#confirmar').disabled = true;
        caixaHorarios.innerHTML = carregando();
        let disp;
        try {
            disp = await api(`/reservas/disponibilidade?areaId=${estado.areaId}&data=${estado.data}`);
        } catch (erro) {
            caixaHorarios.innerHTML = mensagemErro(erro);
            return;
        }
        const abre = minutos(disp.horaAbertura);
        const fecha = minutos(disp.horaFechamento);
        const ocupados = disp.ocupados.map(o => [minutos(o.inicio), minutos(o.fim)]);
        const agora = new Date();
        const limiteHoje = estado.data === hojeIso() ? agora.getHours() * 60 + agora.getMinutes() : -1;

        const livre = t => !ocupados.some(([i, f]) => t >= i && t < f);
        const inicios = [];
        for (let t = abre; t + 30 <= fecha; t += 30) {
            if (livre(t) && t > limiteHoje) inicios.push(t);
        }
        const fimPossivel = ini => {
            const proximo = ocupados.filter(([i]) => i > ini).map(([i]) => i);
            return Math.min(fecha, ...proximo);
        };

        const total = fecha - abre;
        const barra = ocupados.map(([i, f]) =>
            `<div class="bloco-ocupado" style="left:${(i - abre) / total * 100}%;width:${(f - i) / total * 100}%"></div>`).join('');

        caixaHorarios.innerHTML = `
            <div class="ocupacao" id="barra">${barra}<div class="bloco-escolhido oculto" id="escolhido"></div></div>
            <div class="ocupacao-legenda"><span>${esc(disp.horaAbertura)}</span><span>${esc(disp.horaFechamento)}</span></div>
            <div class="legenda-cores">
                <span><i style="background:var(--ok-suave);border:1px solid var(--borda)"></i>Livre</span>
                <span><i style="background:var(--perigo)"></i>Reservado</span>
                <span><i style="border:2px solid var(--destaque)"></i>Sua escolha</span>
            </div>
            ${ocupados.length ? `<p class="pequeno" style="margin:10px 0 0">Ocupado: ${disp.ocupados.map(o => `${esc(o.inicio)}–${esc(o.fim)}`).join(', ')}</p>` : ''}
            ${inicios.length ? `
            <div class="dupla" style="margin-top:14px">
                <div class="campo" style="margin:0">
                    <label for="inicio">Início</label>
                    <select class="entrada" id="inicio" name="horaInicio">
                        <option value="">--:--</option>
                        ${inicios.map(t => `<option value="${t}">${hora(t)}</option>`).join('')}
                    </select>
                </div>
                <div class="campo" style="margin:0">
                    <label for="fim">Fim</label>
                    <select class="entrada" id="fim" name="horaFim" disabled><option value="">--:--</option></select>
                </div>
            </div>` : `<div class="mensagem alerta" style="margin:14px 0 0">${ic('info')}<span>Não há horários livres neste dia. Escolha outra data ou área.</span></div>`}
        `;

        const selInicio = caixaHorarios.querySelector('#inicio');
        const selFim = caixaHorarios.querySelector('#fim');
        const escolhido = caixaHorarios.querySelector('#escolhido');
        if (!selInicio) return;

        function pintar() {
            if (estado.inicio == null || estado.fim == null) {
                escolhido.classList.add('oculto');
                return;
            }
            escolhido.classList.remove('oculto');
            escolhido.style.left = `${(estado.inicio - abre) / total * 100}%`;
            escolhido.style.width = `${(estado.fim - estado.inicio) / total * 100}%`;
        }

        selInicio.addEventListener('change', () => {
            estado.inicio = selInicio.value === '' ? null : Number(selInicio.value);
            estado.fim = null;
            if (estado.inicio == null) {
                selFim.disabled = true;
                selFim.innerHTML = '<option value="">--:--</option>';
            } else {
                const max = fimPossivel(estado.inicio);
                const fins = [];
                for (let t = estado.inicio + 30; t <= max; t += 30) fins.push(t);
                selFim.disabled = false;
                selFim.innerHTML = fins.map(t => `<option value="${t}">${hora(t)}</option>`).join('');
                // sugere 2 horas quando cabe
                const sugestao = fins.find(t => t === estado.inicio + 120) ?? fins[fins.length - 1];
                selFim.value = sugestao;
                estado.fim = sugestao;
            }
            form.querySelector('#confirmar').disabled = estado.fim == null;
            pintar();
        });
        selFim.addEventListener('change', () => {
            estado.fim = Number(selFim.value);
            pintar();
        });
    }

    form.querySelectorAll('input[name=area]').forEach(r => r.addEventListener('change', () => {
        estado.areaId = Number(r.value);
        carregarHorarios();
    }));
    el.querySelector('#dias').addEventListener('click', e => {
        const b = e.target.closest('.dia');
        if (!b) return;
        estado.data = b.dataset.dia;
        marcarDia();
        carregarHorarios();
    });

    form.addEventListener('submit', async e => {
        e.preventDefault();
        const botao = form.querySelector('#confirmar');
        botao.disabled = true;
        try {
            await api('/reservas', {
                metodo: 'POST',
                corpo: {
                    areaId: estado.areaId, data: estado.data,
                    horaInicio: hora(estado.inicio), horaFim: hora(estado.fim),
                    observacao: form.observacao.value || null
                }
            });
            toast('Reserva confirmada!');
            ir('#/reservas');
        } catch (erro) {
            // 409: alguem reservou o horario enquanto a tela estava aberta
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            window.scrollTo({ top: 0, behavior: 'smooth' });
            if (erro.status === 409) carregarHorarios();
            botao.disabled = false;
        }
    });

    marcarDia();
    el.querySelector('.dia.ativo')?.scrollIntoView({ inline: 'center', block: 'nearest' });
    carregarHorarios();
}

// ======================================================= OCORRENCIAS
export async function telaOcorrencias(el) {
    el.innerHTML = carregando();
    let lista;
    try {
        lista = await api('/ocorrencias');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    const sindico = ehSindico();
    const pendentes = fila();
    const abertas = lista.filter(o => o.status !== 'RESOLVIDA');
    const resolvidas = lista.filter(o => o.status === 'RESOLVIDA');
    let aba = 'abertas';

    const itemPendente = p => `
        <li class="item">
            <span class="icone-bola cor-vermelho" style="width:38px;height:38px;border-radius:11px;display:grid;place-items:center">${ic('offline')}</span>
            <div class="meio">
                <div class="titulo">${esc(p.titulo)}</div>
                <div class="sub">Será enviada quando a internet voltar</div>
            </div>
            <span class="status status-PENDENTE">Na fila</span>
        </li>`;

    const item = o => `
        <li><a class="item" href="#/ocorrencias/${o.id}">
            <div class="meio">
                <div class="titulo">${esc(o.titulo)}</div>
                <div class="sub">${esc(o.categoriaDescricao)} · ${esc(relativo(o.abertaEm))}
                    ${sindico ? ` · ${esc(o.morador)} (${esc(o.apartamento)} ${esc(o.bloco)})` : ''}</div>
            </div>
            <span class="status status-${o.status}">${esc(o.statusDescricao)}</span>
            <span class="seta">${ic('seta')}</span>
        </a></li>`;

    function desenhar() {
        const itens = aba === 'abertas' ? abertas : resolvidas;
        const temAlgo = itens.length || (aba === 'abertas' && pendentes.length);
        el.innerHTML = `
            <div class="segmentos" role="tablist">
                <button role="tab" class="${aba === 'abertas' ? 'ativo' : ''}" data-aba="abertas">Em aberto (${abertas.length + pendentes.length})</button>
                <button role="tab" class="${aba === 'resolvidas' ? 'ativo' : ''}" data-aba="resolvidas">Resolvidas (${resolvidas.length})</button>
            </div>
            <div class="cartao">
                ${temAlgo ? `<ul class="lista">
                    ${aba === 'abertas' ? pendentes.map(itemPendente).join('') : ''}
                    ${itens.map(item).join('')}
                </ul>` : vazio('ocorrencias', aba === 'abertas' ? 'Nenhuma ocorrência em aberto.' : 'Nenhuma ocorrência resolvida ainda.')}
            </div>
            ${sindico ? '' : `<a class="fab" href="#/ocorrencias/nova">${ic('mais')}Nova ocorrência</a>`}
        `;
        el.querySelectorAll('[data-aba]').forEach(b => b.addEventListener('click', () => {
            aba = b.dataset.aba;
            desenhar();
        }));
    }
    desenhar();
}

export function telaNovaOcorrencia(el) {
    if (ehSindico()) {
        el.innerHTML = `<div class="mensagem info">${ic('info')}<span>Ocorrências são abertas pelos moradores. Como síndico, você acompanha e responde na lista.</span></div>`;
        return;
    }
    el.innerHTML = `
        <form id="formOcorrencia" novalidate>
            <div id="msg"></div>
            <span class="rotulo">Categoria</span>
            <div class="opcoes campo">
                ${CATEGORIAS_OCORRENCIA.map(([v, t, d], i) => `
                    <label class="opcao">
                        <input type="radio" name="categoria" value="${v}" ${i === 0 ? 'checked' : ''}>
                        <span><strong>${t}</strong><small>${d}</small></span>
                    </label>`).join('')}
            </div>
            <div class="campo">
                <label for="titulo">Resumo do problema</label>
                <input class="entrada" id="titulo" name="titulo" maxlength="120" placeholder="Ex.: Lâmpada queimada no corredor">
            </div>
            <div class="campo">
                <label for="descricao">Descrição</label>
                <textarea class="entrada" id="descricao" name="descricao" maxlength="2000"
                          placeholder="Onde é, desde quando, o que está acontecendo"></textarea>
            </div>
            <p class="pequeno">${ic('info')} Sem internet? Pode enviar mesmo assim: o app guarda e envia sozinho quando a conexão voltar.</p>
            <button class="botao primario bloco" type="submit">${ic('enviar')}Enviar ocorrência</button>
        </form>`;

    const form = el.querySelector('#formOcorrencia');
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const dados = {
            titulo: form.titulo.value.trim(),
            descricao: form.descricao.value.trim(),
            categoria: form.categoria.value
        };
        const botao = form.querySelector('button');
        botao.disabled = true;
        try {
            await api('/ocorrencias', { metodo: 'POST', corpo: dados });
            toast('Ocorrência enviada ao síndico.');
            ir('#/ocorrencias');
        } catch (erro) {
            if (erro.offline && dados.titulo && dados.descricao) {
                enfileirar(dados);
                toast('Sem internet: a ocorrência foi guardada e será enviada automaticamente.');
                window.dispatchEvent(new Event('jcondo:fila'));
                ir('#/ocorrencias');
                return;
            }
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(form, erro.campos ?? (!dados.titulo || !dados.descricao
                ? { titulo: !dados.titulo ? 'Informe um resumo' : undefined, descricao: !dados.descricao ? 'Descreva o problema' : undefined }
                : null));
            botao.disabled = false;
        }
    });
}

export async function telaOcorrencia(el, { id }) {
    el.innerHTML = carregando();
    let o;
    try {
        o = await api(`/ocorrencias/${id}`);
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    const sindico = ehSindico();
    const atualizada = o.atualizadaEm !== o.abertaEm;
    el.innerHTML = `
        <div class="cartao cartao-corpo">
            <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
                <span class="status status-${o.status}">${esc(o.statusDescricao)}</span>
                <span class="chip">${esc(o.categoriaDescricao)}</span>
                <span class="pequeno" style="margin-left:auto">#${o.id}</span>
            </div>
            <h2 style="margin:12px 0 8px">${esc(o.titulo)}</h2>
            <div class="texto-longo">${esc(o.descricao)}</div>
            ${sindico ? `<p class="pequeno" style="margin:14px 0 0">${ic('perfil')} ${esc(o.morador)} · Apto ${esc(o.apartamento)}, bloco ${esc(o.bloco)}</p>` : ''}
        </div>

        <div class="secao-titulo"><h3>Acompanhamento</h3></div>
        <div class="cartao cartao-corpo">
            <ul class="linha-tempo">
                <li><strong>Ocorrência aberta</strong><div class="quando">${esc(dataHoraCurta(o.abertaEm))}</div></li>
                ${atualizada ? `<li><strong>Status: ${esc(o.statusDescricao)}</strong><div class="quando">${esc(dataHoraCurta(o.atualizadaEm))}</div>
                    ${o.resposta ? `<div class="resposta"><div class="pequeno">Resposta do síndico</div>${esc(o.resposta)}</div>` : ''}</li>` : ''}
                ${!atualizada ? `<li><strong>Aguardando o síndico</strong><div class="quando">Você vê aqui quando houver resposta.</div></li>` : ''}
            </ul>
        </div>

        ${sindico ? `
        <div class="secao-titulo"><h3>Responder</h3></div>
        <form class="cartao cartao-corpo" id="formStatus">
            <div id="msg"></div>
            <div class="campo">
                <label for="status">Status</label>
                <select class="entrada" id="status" name="status">
                    <option value="ABERTA" ${o.status === 'ABERTA' ? 'selected' : ''}>Aberta</option>
                    <option value="EM_ANDAMENTO" ${o.status === 'EM_ANDAMENTO' ? 'selected' : ''}>Em andamento</option>
                    <option value="RESOLVIDA" ${o.status === 'RESOLVIDA' ? 'selected' : ''}>Resolvida</option>
                </select>
            </div>
            <div class="campo">
                <label for="resposta">Resposta ao morador</label>
                <textarea class="entrada" id="resposta" name="resposta" maxlength="1000" placeholder="Ex.: técnico agendado"></textarea>
            </div>
            <button class="botao primario bloco" type="submit">${ic('enviar')}Salvar</button>
        </form>` : ''}
    `;

    el.querySelector('#formStatus')?.addEventListener('submit', async e => {
        e.preventDefault();
        const form = e.target;
        try {
            await api(`/ocorrencias/${id}`, {
                metodo: 'PATCH',
                corpo: { status: form.status.value, resposta: form.resposta.value || null }
            });
            toast('Ocorrência atualizada.');
            telaOcorrencia(el, { id });
        } catch (erro) {
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
        }
    });
}

// ======================================================= PERFIL
export async function telaPerfil(el) {
    const u = usuario();
    const sindico = u.perfil === 'SINDICO';
    const instalado = matchMedia('(display-mode: standalone)').matches || navigator.standalone;

    el.innerHTML = `
        <div class="cartao cartao-corpo" style="display:flex;gap:16px;align-items:center">
            <div class="avatar">${esc(iniciais(u.nome))}</div>
            <div style="min-width:0">
                <h2 style="margin:0;font-size:1.2rem">${esc(u.nome)}</h2>
                <div class="pequeno" style="word-break:break-all">${esc(u.email)}</div>
            </div>
        </div>

        <div class="secao-titulo"><h3>Meus dados</h3></div>
        <div class="cartao cartao-corpo">
            <dl class="dados">
                <dt>Perfil</dt><dd>${sindico ? 'Síndico' : 'Morador'}</dd>
                ${u.morador ? `
                <dt>Apartamento</dt><dd>${esc(u.morador.apartamento)}</dd>
                <dt>Bloco</dt><dd>${esc(u.morador.bloco)}</dd>
                <dt>Telefone</dt><dd>${esc(u.morador.telefone)}</dd>` : ''}
            </dl>
        </div>

        <div class="secao-titulo"><h3>Aplicativo</h3></div>
        <div class="cartao">
            <div id="linhaInstalar"></div>
            <button class="linha-acao" id="abrirSenha">${ic('chave')}<span>Trocar senha</span>${ic('seta')}</button>
            <form id="formSenha" class="cartao-corpo oculto" novalidate style="border-bottom:1px solid var(--borda)">
                <div id="msgSenha"></div>
                <div class="campo"><label for="senhaAtual">Senha atual</label>
                    <input class="entrada" type="password" id="senhaAtual" name="senhaAtual" autocomplete="current-password"></div>
                <div class="campo"><label for="novaSenha">Nova senha</label>
                    <input class="entrada" type="password" id="novaSenha" name="novaSenha" autocomplete="new-password" minlength="6"></div>
                <button class="botao primario bloco" type="submit">Salvar nova senha</button>
            </form>
            ${sindico ? `<a class="linha-acao" href="/painel">${ic('web')}<span>Abrir painel web do síndico</span>${ic('seta')}</a>` : ''}
            <a class="linha-acao" href="/swagger-ui/index.html" target="_blank" rel="noopener">${ic('info')}<span>Documentação da API</span>${ic('seta')}</a>
            <button class="linha-acao perigo" id="sair">${ic('sair')}<span>Sair</span></button>
        </div>

        <p class="pequeno" style="text-align:center;margin-top:18px">
            JCondo v2.0 · ${instalado ? 'App instalado' : 'Rodando no navegador'} ·
            <span id="statusConexao">${navigator.onLine ? 'online' : 'offline'}</span>
        </p>`;

    instalacao.desenharLinha(el.querySelector('#linhaInstalar'));

    el.querySelector('#abrirSenha').addEventListener('click', () => {
        el.querySelector('#formSenha').classList.toggle('oculto');
    });
    el.querySelector('#formSenha').addEventListener('submit', async e => {
        e.preventDefault();
        const f = e.target;
        try {
            await api('/auth/senha', { metodo: 'PUT', corpo: { senhaAtual: f.senhaAtual.value, novaSenha: f.novaSenha.value } });
            f.reset();
            f.classList.add('oculto');
            toast('Senha alterada.');
        } catch (erro) {
            el.querySelector('#msgSenha').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(f, erro.campos);
        }
    });
    el.querySelector('#sair').addEventListener('click', () => {
        if (!confirm('Sair do JCondo neste aparelho?')) return;
        sair();
        ir('#/login');
    });
}
