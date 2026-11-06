// telas do app do morador. Cada tela recebe o elemento onde desenhar e os parametros da rota
import { api, entrar, enfileirar, fila, sair, usuario } from './api.js';
import {
    blocoData, carregando, dataHoraCurta, diaSemana, esc, hojeIso, hora, ic, iniciais, isoDia,
    mensagemErro, minutos, mostrarErrosCampos, paraData, primeiroNome, relativo, somarDias, toast, vazio
} from './util.js';
import { instalacao } from './instalar.js';

const CATEGORIAS = [
    ['MANUTENCAO', 'Manutenção'], ['BARULHO', 'Barulho'], ['SEGURANCA', 'Segurança'],
    ['LIMPEZA', 'Limpeza'], ['OUTROS', 'Outros']
];

function ir(rota) {
    location.hash = rota;
}

function situacaoReserva(r) {
    if (r.status === 'CANCELADA') return ['CANCELADA', 'Cancelada'];
    if (r.data < hojeIso()) return ['REALIZADA', 'Realizada'];
    return ['CONFIRMADA', 'Confirmada'];
}

function itemReserva(r) {
    const [cls, texto] = situacaoReserva(r);
    return `
        <li class="item">
            ${blocoData(r.data)}
            <div class="meio">
                <div class="titulo">${esc(r.area)}</div>
                <div class="sub">${esc(diaSemana(paraData(r.data)))}, ${esc(r.horaInicio)} às ${esc(r.horaFim)}</div>
            </div>
            <span class="status status-${cls}">${texto}</span>
        </li>`;
}

function cartaoAviso(a) {
    return `
        <a class="cartao aviso ${a.importante ? 'importante' : ''}" href="#/avisos/${a.id}">
            <div class="linha-topo">
                <span class="chip">${esc(a.categoriaDescricao)}</span>
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
            </div>
            <div class="login-form">
                <div class="caixa">
                    <h2 style="margin:0 0 20px">Entrar</h2>
                    <div id="msg"></div>
                    <form id="formLogin" novalidate>
                        <div class="campo">
                            <label for="email">E-mail</label>
                            <input class="entrada" type="email" id="email" name="email" autocomplete="username" inputmode="email" required>
                        </div>
                        <div class="campo">
                            <label for="senha">Senha</label>
                            <input class="entrada" type="password" id="senha" name="senha" autocomplete="current-password" required>
                        </div>
                        <button class="botao primario bloco" type="submit">Entrar</button>
                    </form>
                </div>
            </div>
        </div>`;

    const form = el.querySelector('#formLogin');
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const botao = form.querySelector('button[type=submit]');
        botao.disabled = true;
        try {
            const u = await entrar(form.email.value.trim(), form.senha.value);
            // o app e do morador; o sindico usa o painel web
            if (u.perfil !== 'MORADOR') {
                sair();
                throw new Error('Este app é para moradores. O síndico usa o painel web.');
            }
            ir('#/inicio');
        } catch (erro) {
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(form, erro.campos);
            botao.disabled = false;
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
    const pendentes = fila().length;

    el.innerHTML = `
        <div class="saudacao">
            <h2>Olá, ${esc(primeiroNome(u.nome))}</h2>
            <p>Apartamento ${esc(u.morador?.apartamento)} · Bloco ${esc(u.morador?.bloco)}</p>
        </div>

        ${pendentes ? `<div class="mensagem alerta">${ic('offline')}<span>${pendentes} ocorrência(s) aguardando internet.</span></div>` : ''}

        <div class="numeros">
            <a class="cartao numero" href="#/reservas">
                <span class="icone-bola cor-verde">${ic('reservas')}</span>
                <strong>${r.reservasFuturas}</strong><span>Reservas</span>
            </a>
            <a class="cartao numero" href="#/ocorrencias">
                <span class="icone-bola cor-ambar">${ic('ocorrencias')}</span>
                <strong>${r.ocorrenciasEmAberto}</strong><span>Ocorrências em aberto</span>
            </a>
        </div>

        <div class="secao-titulo"><h3>Próximas reservas</h3><a href="#/reservas">Ver todas</a></div>
        <div class="cartao">
            ${r.proximasReservas.length
                ? `<ul class="lista">${r.proximasReservas.map(itemReserva).join('')}</ul>`
                : vazio('reservas', 'Nenhuma reserva marcada.')}
        </div>

        <div class="secao-titulo"><h3>Avisos recentes</h3><a href="#/avisos">Ver todos</a></div>
        ${r.ultimosAvisos.length ? r.ultimosAvisos.map(cartaoAviso).join('') : `<div class="cartao">${vazio('avisos', 'Nenhum aviso.')}</div>`}
    `;
}

// ======================================================= AVISOS
export async function telaAvisos(el) {
    el.innerHTML = carregando();
    try {
        const avisos = await api('/avisos');
        el.innerHTML = avisos.length
            ? avisos.map(cartaoAviso).join('')
            : `<div class="cartao">${vazio('avisos', 'Nenhum aviso.')}</div>`;
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
    }
}

export async function telaAviso(el, { id }) {
    el.innerHTML = carregando();
    try {
        const a = await api(`/avisos/${id}`);
        el.innerHTML = `
            <article class="cartao cartao-corpo aviso ${a.importante ? 'importante' : ''}">
                <span class="chip">${esc(a.categoriaDescricao)}</span>
                <h2 style="margin:12px 0 4px">${esc(a.titulo)}</h2>
                <p class="pequeno" style="margin:0 0 16px">${esc(dataHoraCurta(a.publicadoEm))}</p>
                <div class="texto-longo">${esc(a.conteudo)}</div>
            </article>`;
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
    }
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
                <button role="tab" class="${aba === 'proximas' ? 'ativo' : ''}" data-aba="proximas">Próximas</button>
                <button role="tab" class="${aba === 'historico' ? 'ativo' : ''}" data-aba="historico">Histórico</button>
            </div>
            <div class="cartao">
                ${lista.length ? `<ul class="lista">${lista.map(r => `
                    <li class="item">
                        ${blocoData(r.data)}
                        <div class="meio">
                            <div class="titulo">${esc(r.area)}</div>
                            <div class="sub">${esc(diaSemana(paraData(r.data)))}, ${esc(r.horaInicio)} às ${esc(r.horaFim)}</div>
                        </div>
                        ${aba === 'proximas'
                            ? `<button class="botao perigo" style="min-height:38px;padding:0 12px" data-cancelar="${r.id}" aria-label="Cancelar reserva">${ic('x')}</button>`
                            : `<span class="status status-${situacaoReserva(r)[0]}">${situacaoReserva(r)[1]}</span>`}
                    </li>`).join('')}</ul>`
                : vazio('reservas', 'Nenhuma reserva.')}
            </div>
            <a class="fab" href="#/reservas/nova">${ic('mais')}Reservar</a>
        `;
        el.querySelectorAll('[data-aba]').forEach(b => b.addEventListener('click', () => {
            aba = b.dataset.aba;
            desenhar();
        }));
        el.querySelectorAll('[data-cancelar]').forEach(b => b.addEventListener('click', async () => {
            if (!confirm('Cancelar esta reserva?')) return;
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
    el.innerHTML = carregando();
    let areas;
    try {
        areas = await api('/areas');
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
        return;
    }
    if (!areas.length) {
        el.innerHTML = vazio('predio', 'Nenhuma área disponível.');
        return;
    }

    const estado = { areaId: areas[0].id, data: isoDia(somarDias(new Date(), 1)), inicio: null, fim: null };
    const dias = Array.from({ length: 30 }, (_, i) => somarDias(new Date(), i));

    el.innerHTML = `
        <form id="formReserva" novalidate>
            <div id="msg"></div>
            <span class="rotulo">Área</span>
            <div class="opcoes" style="margin-bottom:18px">
                ${areas.map((a, i) => `
                    <label class="opcao">
                        <input type="radio" name="area" value="${a.id}" ${i === 0 ? 'checked' : ''}>
                        <span><strong>${esc(a.nome)}</strong><small>${esc(a.horaAbertura)} às ${esc(a.horaFechamento)}</small></span>
                    </label>`).join('')}
            </div>

            <span class="rotulo">Dia</span>
            <div class="dias" id="dias" style="margin-bottom:12px">
                ${dias.map(d => `
                    <button type="button" class="dia" data-dia="${isoDia(d)}">
                        <small>${esc(diaSemana(d))}</small><strong>${d.getDate()}</strong>
                    </button>`).join('')}
            </div>

            <span class="rotulo">Horário</span>
            <div class="cartao cartao-corpo" id="horarios" style="margin-bottom:16px">${carregando()}</div>

            <button class="botao primario bloco" type="submit" id="confirmar" disabled>${ic('check')}Confirmar reserva</button>
        </form>`;

    const form = el.querySelector('#formReserva');
    const caixaHorarios = el.querySelector('#horarios');

    function marcarDia() {
        el.querySelectorAll('.dia').forEach(b => b.classList.toggle('ativo', b.dataset.dia === estado.data));
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
        const fimPossivel = ini => Math.min(fecha, ...ocupados.filter(([i]) => i > ini).map(([i]) => i));

        const total = fecha - abre;
        const barra = ocupados.map(([i, f]) =>
            `<div class="bloco-ocupado" style="left:${(i - abre) / total * 100}%;width:${(f - i) / total * 100}%"></div>`).join('');

        caixaHorarios.innerHTML = `
            <div class="ocupacao">${barra}<div class="bloco-escolhido oculto" id="escolhido"></div></div>
            <div class="ocupacao-legenda"><span>${esc(disp.horaAbertura)}</span><span>${esc(disp.horaFechamento)}</span></div>
            <div class="legenda-cores">
                <span><i style="background:var(--ok-suave);border:1px solid var(--borda)"></i>Livre</span>
                <span><i style="background:var(--perigo)"></i>Reservado</span>
            </div>
            ${inicios.length ? `
            <div class="dupla" style="margin-top:14px">
                <div class="campo" style="margin:0">
                    <label for="inicio">Início</label>
                    <select class="entrada" id="inicio">
                        <option value="">--:--</option>
                        ${inicios.map(t => `<option value="${t}">${hora(t)}</option>`).join('')}
                    </select>
                </div>
                <div class="campo" style="margin:0">
                    <label for="fim">Fim</label>
                    <select class="entrada" id="fim" disabled><option value="">--:--</option></select>
                </div>
            </div>` : `<div class="mensagem alerta" style="margin:14px 0 0">${ic('info')}<span>Sem horários livres neste dia.</span></div>`}
        `;

        const selInicio = caixaHorarios.querySelector('#inicio');
        const selFim = caixaHorarios.querySelector('#fim');
        const escolhido = caixaHorarios.querySelector('#escolhido');
        if (!selInicio) return;

        function pintar() {
            const ok = estado.inicio != null && estado.fim != null;
            escolhido.classList.toggle('oculto', !ok);
            if (ok) {
                escolhido.style.left = `${(estado.inicio - abre) / total * 100}%`;
                escolhido.style.width = `${(estado.fim - estado.inicio) / total * 100}%`;
            }
        }

        selInicio.addEventListener('change', () => {
            estado.inicio = selInicio.value === '' ? null : Number(selInicio.value);
            estado.fim = null;
            if (estado.inicio == null) {
                selFim.disabled = true;
                selFim.innerHTML = '<option value="">--:--</option>';
            } else {
                const fins = [];
                for (let t = estado.inicio + 30; t <= fimPossivel(estado.inicio); t += 30) fins.push(t);
                selFim.disabled = false;
                selFim.innerHTML = fins.map(t => `<option value="${t}">${hora(t)}</option>`).join('');
                // sugere 2 horas quando cabe
                estado.fim = fins.find(t => t === estado.inicio + 120) ?? fins[fins.length - 1];
                selFim.value = estado.fim;
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
                corpo: { areaId: estado.areaId, data: estado.data, horaInicio: hora(estado.inicio), horaFim: hora(estado.fim) }
            });
            toast('Reserva confirmada.');
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
    const pendentes = fila();
    const itens = [
        ...pendentes.map(p => `
            <li class="item">
                <div class="meio">
                    <div class="titulo">${esc(p.titulo)}</div>
                    <div class="sub">Aguardando internet</div>
                </div>
                <span class="status status-PENDENTE">Na fila</span>
            </li>`),
        ...lista.map(o => `
            <li><a class="item" href="#/ocorrencias/${o.id}">
                <div class="meio">
                    <div class="titulo">${esc(o.titulo)}</div>
                    <div class="sub">${esc(o.categoriaDescricao)} · ${esc(relativo(o.abertaEm))}</div>
                </div>
                <span class="status status-${o.status}">${esc(o.statusDescricao)}</span>
            </a></li>`)
    ];
    el.innerHTML = `
        <div class="cartao">
            ${itens.length ? `<ul class="lista">${itens.join('')}</ul>` : vazio('ocorrencias', 'Nenhuma ocorrência.')}
        </div>
        <a class="fab" href="#/ocorrencias/nova">${ic('mais')}Nova ocorrência</a>
    `;
}

export function telaNovaOcorrencia(el) {
    el.innerHTML = `
        <form id="formOcorrencia" novalidate>
            <div id="msg"></div>
            <span class="rotulo">Categoria</span>
            <div class="opcoes campo">
                ${CATEGORIAS.map(([v, t], i) => `
                    <label class="opcao">
                        <input type="radio" name="categoria" value="${v}" ${i === 0 ? 'checked' : ''}>
                        <span><strong>${t}</strong></span>
                    </label>`).join('')}
            </div>
            <div class="campo">
                <label for="titulo">Título</label>
                <input class="entrada" id="titulo" name="titulo" maxlength="120">
            </div>
            <div class="campo">
                <label for="descricao">Descrição</label>
                <textarea class="entrada" id="descricao" name="descricao" maxlength="2000"></textarea>
            </div>
            <button class="botao primario bloco" type="submit">${ic('enviar')}Enviar</button>
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
            toast('Ocorrência enviada.');
            ir('#/ocorrencias');
        } catch (erro) {
            // sem internet: guarda e envia quando a conexao voltar
            if (erro.offline && dados.titulo && dados.descricao) {
                enfileirar(dados);
                toast('Sem internet: será enviada quando a conexão voltar.');
                window.dispatchEvent(new Event('jcondo:fila'));
                ir('#/ocorrencias');
                return;
            }
            el.querySelector('#msg').innerHTML = mensagemErro(erro);
            mostrarErrosCampos(form, erro.campos);
            botao.disabled = false;
        }
    });
}

export async function telaOcorrencia(el, { id }) {
    el.innerHTML = carregando();
    try {
        const o = await api(`/ocorrencias/${id}`);
        el.innerHTML = `
            <div class="cartao cartao-corpo">
                <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
                    <span class="status status-${o.status}">${esc(o.statusDescricao)}</span>
                    <span class="chip">${esc(o.categoriaDescricao)}</span>
                </div>
                <h2 style="margin:12px 0 8px">${esc(o.titulo)}</h2>
                <div class="texto-longo">${esc(o.descricao)}</div>
                <p class="pequeno" style="margin:14px 0 0">Aberta em ${esc(dataHoraCurta(o.abertaEm))}</p>
            </div>
            ${o.resposta ? `
            <div class="secao-titulo"><h3>Resposta do síndico</h3></div>
            <div class="cartao cartao-corpo">
                <div class="texto-longo">${esc(o.resposta)}</div>
                <p class="pequeno" style="margin:10px 0 0">${esc(dataHoraCurta(o.atualizadaEm))}</p>
            </div>` : ''}`;
    } catch (erro) {
        el.innerHTML = mensagemErro(erro);
    }
}

// ======================================================= PERFIL
export function telaPerfil(el) {
    const u = usuario();
    el.innerHTML = `
        <div class="cartao cartao-corpo" style="display:flex;gap:16px;align-items:center">
            <div class="avatar">${esc(iniciais(u.nome))}</div>
            <div style="min-width:0">
                <h2 style="margin:0;font-size:1.2rem">${esc(u.nome)}</h2>
                <div class="pequeno" style="word-break:break-all">${esc(u.email)}</div>
            </div>
        </div>

        <div class="cartao cartao-corpo" style="margin-top:12px">
            <dl class="dados">
                <dt>Apartamento</dt><dd>${esc(u.morador?.apartamento)}</dd>
                <dt>Bloco</dt><dd>${esc(u.morador?.bloco)}</dd>
                <dt>Telefone</dt><dd>${esc(u.morador?.telefone)}</dd>
            </dl>
        </div>

        <div class="cartao" style="margin-top:12px">
            <div id="linhaInstalar"></div>
            <button class="linha-acao perigo" id="sair">${ic('sair')}<span>Sair</span></button>
        </div>`;

    instalacao.desenharLinha(el.querySelector('#linhaInstalar'));
    el.querySelector('#sair').addEventListener('click', () => {
        sair();
        ir('#/login');
    });
}
