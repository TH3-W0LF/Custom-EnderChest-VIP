package com.alkacode.enderchest.listener;

import com.alkacode.enderchest.util.Messages;
import com.alkacode.enderchest.util.PasswordGate;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Guarda-redes minimo, NAO o mecanismo de seguranca em si: se o jogador tem um prompt
 * de senha pendente (PasswordGate#isPending) e digita qualquer coisa no chat normal em
 * vez de rodar /ec entrar <senha> (por habito/distracao), cancela a mensagem e lembra
 * o comando certo. Nunca le, faz hash ou processa o texto como tentativa de senha -
 * isso e responsabilidade exclusiva de PasswordGate#handleAttempt, chamado a partir do
 * comando.
 *
 * Importante: isso NAO da a mesma garantia contra vazamento que o comando da. Cancelar
 * aqui ainda depende de rodar antes de qualquer outro plugin de chat processar a
 * mensagem - o padrao e ignoreCancelled=false, entao um plugin de chat local mal
 * comportado (ou que registre na mesma prioridade LOWEST, cuja ordem entre plugins nao
 * e garantida) ainda pode ver o texto antes de cancelarmos. Ver o historico completo
 * das tentativas anteriores (bigorna, depois chat puro) no javadoc de PasswordGate. O
 * unico caminho realmente imune a esse problema e o comando - este listener existe so
 * para reduzir a chance de exposicao acidental, nao para substituir o comando.
 */
public class PasswordChatGuardListener implements Listener {

    private final PasswordGate passwordGate;
    private final Messages messages;

    public PasswordChatGuardListener(PasswordGate passwordGate, Messages messages) {
        this.passwordGate = passwordGate;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!passwordGate.isPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(messages.get("password.chat-guard"));
    }
}
