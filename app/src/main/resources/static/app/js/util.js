// funcoes de apoio usadas pelas telas

// escapa texto antes de colocar no HTML (evita XSS com o que vem da API)
export function esc(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

export function ic(nome) {
    return `<svg class="ic" aria-hidden="true"><use href="#i-${nome}"/></svg>`;
}

// ---------- datas ----------
// a API manda LocalDate "2026-11-08" e LocalDateTime "2026-11-05T15:53:00" no horario de Brasilia
export function paraData(iso) {
    const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
    return new Date(a, m - 1, d);
}

export function paraDataHora(iso) {
    const [data, hora = '00:00'] = iso.split('T');
    const [a, m, d] = data.split('-').map(Number);
    const [h, min] = hora.split(':').map(Number);
    return new Date(a, m - 1, d, h, min);
}

export function isoDia(data) {
    const p = n => String(n).padStart(2, '0');
    return `${data.getFullYear()}-${p(data.getMonth() + 1)}-${p(data.getDate())}`;
}

export function hojeIso() {
    return isoDia(new Date());
}

export function somarDias(data, dias) {
    const d = new Date(data);
    d.setDate(d.getDate() + dias);
    return d;
}

const fmtDiaSemana = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' });
const fmtMes = new Intl.DateTimeFormat('pt-BR', { month: 'short' });
const fmtCurta = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
const fmtHora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

const semPonto = s => s.replace('.', '');

export function diaSemana(data) {
    return semPonto(fmtDiaSemana.format(data));
}

export function mesCurto(data) {
    return semPonto(fmtMes.format(data));
}

export function dataHoraCurta(iso) {
    const d = paraDataHora(iso);
    return `${fmtCurta.format(d)} às ${fmtHora.format(d)}`;
}

// "agora", "ha 3 h", "ontem", "ha 4 dias"
export function relativo(iso) {
    const d = paraDataHora(iso);
    const minutos = Math.round((Date.now() - d.getTime()) / 60000);
    if (minutos < 1) return 'agora';
    if (minutos < 60) return `há ${minutos} min`;
    const horas = Math.round(minutos / 60);
    if (horas < 24) return `há ${horas} h`;
    const dias = Math.round(horas / 24);
    if (dias === 1) return 'ontem';
    if (dias < 7) return `há ${dias} dias`;
    return fmtCurta.format(d);
}

export function blocoData(iso) {
    const d = paraData(iso);
    return `<div class="data-bloco"><strong>${String(d.getDate()).padStart(2, '0')}</strong><span>${esc(mesCurto(d))}</span></div>`;
}

// "14:00" -> 840
export function minutos(hora) {
    const [h, m] = hora.split(':').map(Number);
    return h * 60 + m;
}

export function hora(min) {
    return `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`;
}

export function primeiroNome(nome) {
    return (nome || '').split(' ')[0];
}

export function iniciais(nome) {
    const partes = (nome || '?').trim().split(/\s+/);
    return (partes[0][0] + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
}

// ---------- feedback ----------
let timerToast;

export function toast(mensagem, acao) {
    let el = document.querySelector('.toast');
    if (!el) {
        el = document.createElement('div');
        el.className = 'toast';
        el.setAttribute('role', 'status');
        el.setAttribute('aria-live', 'polite');
        document.body.appendChild(el);
    }
    el.innerHTML = `<span>${esc(mensagem)}</span>${acao ? `<button type="button">${esc(acao.texto)}</button>` : ''}`;
    if (acao) {
        el.querySelector('button').onclick = () => {
            el.classList.remove('visivel');
            acao.aoClicar();
        };
    }
    requestAnimationFrame(() => el.classList.add('visivel'));
    clearTimeout(timerToast);
    timerToast = setTimeout(() => el.classList.remove('visivel'), acao ? 10000 : 3500);
}

export function carregando() {
    return '<div class="spinner" role="status" aria-label="Carregando"></div>';
}

export function mensagemErro(erro) {
    const texto = erro?.offline
        ? 'Sem conexão e sem dados salvos desta tela. Conecte-se à internet e tente de novo.'
        : (erro?.message || 'Algo deu errado.');
    return `<div class="mensagem erro">${ic('info')}<span>${esc(texto)}</span></div>`;
}

export function vazio(icone, texto) {
    return `<div class="vazio">${ic(icone)}<div>${esc(texto)}</div></div>`;
}

// mostra os erros de validacao (400) embaixo de cada campo
export function mostrarErrosCampos(form, campos) {
    form.querySelectorAll('.erro-campo').forEach(e => e.remove());
    form.querySelectorAll('.invalida').forEach(e => e.classList.remove('invalida'));
    Object.entries(campos || {}).forEach(([nome, msg]) => {
        const input = form.querySelector(`[name="${nome}"]`);
        if (!input) return;
        input.classList.add('invalida');
        const div = document.createElement('div');
        div.className = 'erro-campo';
        div.textContent = msg;
        (input.closest('.campo') || input.parentElement).appendChild(div);
    });
}
