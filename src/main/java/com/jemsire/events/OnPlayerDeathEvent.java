package com.jemsire.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.jemsire.config.DeathConfig;
import com.jemsire.plugin.JemDeaths;
import com.jemsire.utils.ChatBroadcaster;
import com.jemsire.utils.Logger;
import com.jemsire.utils.PlaceholderReplacer;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class OnPlayerDeathEvent extends DeathSystems.OnDeathSystem {
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        try {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
            DeathComponent deathComponent = (DeathComponent) store.getComponent(ref, DeathComponent.getComponentType());

            if (playerComponent == null || deathComponent == null || playerRef == null) {
                return;
            }

            JemDeaths plugin = JemDeaths.get();
            if (plugin == null) {
                return;
            }

            DeathConfig deathConfig = plugin.getDeathConfig().get();
            if (deathConfig == null) {
                return;
            }

            // Process death cause
            Message rawDeathMessage = deathComponent.getDeathMessage();
            String rawDeathCause;
            try {
                assert rawDeathMessage != null;
                rawDeathCause = rawDeathMessage.getAnsiMessage();
            } catch (Exception e) {
                Logger.severe("Error getting death message: " + e.getMessage(), e);
                return;
            }
            if(rawDeathCause.isEmpty()){
                return;
            }

            String deathCause = formatDeathCause(rawDeathCause, deathConfig);

            // Process position
            double x = 0.0, y = 0.0, z = 0.0;
            String positionString = "Unknown";
            try {
                Transform transform = playerRef.getTransform();
                x = transform.getPosition().x;
                y = transform.getPosition().y;
                z = transform.getPosition().z;
                positionString = String.format("%.1f, %.1f, %.1f", x, y, z);
            } catch (Exception e) {
                Logger.warning("Failed to get player position: " + e.getMessage());
            }

            // Prepare placeholders
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerComponent.getDisplayName());
            placeholders.put("deathCause", deathCause);
            placeholders.put("rawDeathCause", rawDeathCause);
            placeholders.put("position", positionString);
            placeholders.put("x", String.format("%.1f", x));
            placeholders.put("y", String.format("%.1f", y));
            placeholders.put("z", String.format("%.1f", z));

            // Broadcast death message
            if (deathConfig.isShowDeathMessage()) {
                try {
                    String deathMessage = PlaceholderReplacer.replacePlaceholders(
                            deathConfig.getDeathAnnouncementFormat(),
                            placeholders
                    );
                    ChatBroadcaster.broadcastToAll(deathMessage);
                } catch (Exception e) {
                    Logger.severe("Failed to broadcast death message: " + e.getMessage());
                }
            }

            // Send position message
            if (deathConfig.isShowPosition()) {
                try {
                    String positionMessage = PlaceholderReplacer.replacePlaceholders(
                            deathConfig.getDeathLocationFormat(),
                            placeholders
                    );
                    ChatBroadcaster.sendToPlayer(playerRef, positionMessage);
                } catch (Exception e) {
                    Logger.severe("Failed to send position message: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.severe("Error in OnPlayerDeathEvent: " + e.getMessage(), e);
        }
    }

    private String formatDeathCause(String rawCause, DeathConfig config) {
        if (config == null || config.getDeathCauseReplacement() == null) {
            return rawCause;
        }
        return rawCause.replace("You were", config.getDeathCauseReplacement());
    }
}
