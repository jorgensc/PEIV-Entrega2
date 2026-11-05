// scripts pequenos do painel do sindico
document.addEventListener('DOMContentLoaded', () => {

    // confirmacao antes de excluir/cancelar
    document.querySelectorAll('form[data-confirmar]').forEach(form => {
        form.addEventListener('submit', e => {
            if (!confirm(form.dataset.confirmar)) {
                e.preventDefault();
            }
        });
    });

    // busca instantanea nas listas
    document.querySelectorAll('[data-filtro]').forEach(input => {
        const alvo = document.querySelector(input.dataset.filtro);
        const contador = document.querySelector(input.dataset.contador);
        if (!alvo) return;
        input.addEventListener('input', () => {
            const termo = input.value.trim().toLowerCase();
            let visiveis = 0;
            alvo.querySelectorAll('[data-busca]').forEach(item => {
                const ok = item.dataset.busca.toLowerCase().includes(termo);
                item.hidden = !ok;
                if (ok) visiveis++;
            });
            if (contador) contador.textContent = visiveis;
        });
    });

    // QR code apontando pro app do morador (usa o endereco atual, local ou Azure)
    document.querySelectorAll('[data-qr]').forEach(el => {
        if (typeof qrcode === 'undefined') return;
        const url = new URL(el.dataset.qr, window.location.origin).href;
        const qr = qrcode(0, 'M');
        qr.addData(url);
        qr.make();
        el.innerHTML = qr.createSvgTag({ cellSize: 4, margin: 0, scalable: true });
        const legenda = document.querySelector('[data-qr-url]');
        if (legenda) legenda.textContent = url.replace(/^https?:\/\//, '');
    });

    // botoes de usuario de demonstracao na tela de login
    document.querySelectorAll('[data-demo-email]').forEach(botao => {
        botao.addEventListener('click', () => {
            document.getElementById('email').value = botao.dataset.demoEmail;
            document.getElementById('senha').value = botao.dataset.demoSenha;
            document.getElementById('senha').focus();
        });
    });
});
