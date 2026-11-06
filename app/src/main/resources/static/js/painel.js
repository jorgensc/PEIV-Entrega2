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

    // busca na lista de moradores
    document.querySelectorAll('[data-filtro]').forEach(input => {
        const alvo = document.querySelector(input.dataset.filtro);
        if (!alvo) return;
        input.addEventListener('input', () => {
            const termo = input.value.trim().toLowerCase();
            alvo.querySelectorAll('[data-busca]').forEach(item => {
                item.hidden = !item.dataset.busca.toLowerCase().includes(termo);
            });
        });
    });
});
