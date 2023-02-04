package xyz.kyngs.librelogin.bungeecord;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import xyz.kyngs.librelogin.api.authorization.AuthorizationProvider;
import xyz.kyngs.librelogin.api.configuration.PluginConfiguration;

public class Blockers implements Listener {

    private final AuthorizationProvider<ProxiedPlayer> authorizationProvider;
    private final PluginConfiguration configuration;
    private final BungeeCordLibreLogin plugin;

    public Blockers(BungeeCordLibreLogin plugin) {
        this.authorizationProvider = plugin.getAuthorizationProvider();
        this.configuration = plugin.getConfiguration();
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(ChatEvent event) {
        if (event.isCommand()) {
            onCommand(event);
            return;
        }

        if (event.getSender() instanceof ProxiedPlayer player) {
            if (!authorizationProvider.isAuthorized(player) || authorizationProvider.isAwaiting2FA(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;
        if (authorizationProvider.isAuthorized(player) && !authorizationProvider.isAwaiting2FA(player))
            return;

        var command = event.getMessage().substring(1).split(" ")[0];

        for (String allowed : configuration.getAllowedCommandsWhileUnauthorized()) {
            if (command.startsWith(allowed)) return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerConnect(ServerConnectEvent event) {
        if (!authorizationProvider.isAuthorized(event.getPlayer()) && event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
            event.setCancelled(true);
            event.getPlayer().disconnect(plugin.getSerializer().serialize(plugin.getMessages().getMessage("kick-no-server")));
        } else if (authorizationProvider.isAwaiting2FA(event.getPlayer())) {
            if (!configuration.getLimbo().contains(event.getTarget().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerKick(ServerKickEvent event) {
        if (!authorizationProvider.isAuthorized(event.getPlayer()) || authorizationProvider.isAwaiting2FA(event.getPlayer())) {
            var reason = event.getKickReasonComponent();
            if (reason == null) {
                event.getPlayer().disconnect("Limbo shutdown");
            } else {
                event.getPlayer().disconnect(reason);
            }
        }
    }

}