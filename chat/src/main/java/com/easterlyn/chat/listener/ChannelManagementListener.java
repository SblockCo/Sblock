package com.easterlyn.chat.listener;

import com.easterlyn.EasterlynChat;
import com.easterlyn.EasterlynCore;
import com.easterlyn.chat.channel.Channel;
import com.easterlyn.chat.event.UserChatEvent;
import com.easterlyn.event.UserCreationEvent;
import com.easterlyn.user.AutoUser;
import com.easterlyn.user.User;
import com.easterlyn.util.StringUtil;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChannelManagementListener implements Listener {

  private final EasterlynChat chat;

  public ChannelManagementListener(EasterlynChat chat) {
    this.chat = chat;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    RegisteredServiceProvider<EasterlynCore> easterlynRSP =
        chat.getServer().getServicesManager().getRegistration(EasterlynCore.class);
    if (easterlynRSP == null) {
      event
          .getPlayer()
          .sendMessage(
              "Easterlyn core plugin is not enabled! "
                  + "Please report this to @Staff on Discord immediately!");
      return;
    }

    event.setCancelled(true);

    EasterlynCore core = easterlynRSP.getProvider();
    User user = core.getUserManager().getUser(event.getPlayer().getUniqueId());
    event.getPlayer().setDisplayName(user.getDisplayName());

    Channel channel = null;

    // #channel message parsing
    if (event.getMessage().length() > 0 && event.getMessage().charAt(0) == '#') {
      int space = event.getMessage().indexOf(' ');
      if (space == -1) {
        space = event.getMessage().length();
      }
      String channelName = event.getMessage().substring(1, space);
      if (space == event.getMessage().length()) {
        core.getLocaleManager()
            .sendMessage(event.getPlayer(), "chat.common.no_content", "{value}", '#' + channelName);
        return;
      }
      channel = chat.getChannels().get(channelName);
      if (channel == null) {
        core.getLocaleManager()
            .sendMessage(
                event.getPlayer(), "chat.common.no_matching_channel", "{value}", '#' + channelName);
        return;
      }
      if (!user.getStorage()
          .getStringList(EasterlynChat.USER_CHANNELS)
          .contains(channel.getName())) {
        core.getLocaleManager()
            .sendMessage(
                event.getPlayer(),
                "chat.common.not_listening_to_channel",
                "{value}",
                channel.getDisplayName());
        return;
      }
      event.setMessage(event.getMessage().substring(space + 1));
    }

    // User's channel
    if (channel == null) {
      String currentChannelName = user.getStorage().getString(EasterlynChat.USER_CURRENT);
      if (currentChannelName != null) {
        channel = chat.getChannels().get(currentChannelName);
      }
      if (channel == null) {
        core.getLocaleManager()
            .sendMessage(event.getPlayer(), "chat.common.not_listening_to_channel");
        return;
      }
    }

    new UserChatEvent(user, channel, event.getMessage()).send();
  }

  @EventHandler
  public void onUserCreate(UserCreationEvent event) {
    RegisteredServiceProvider<EasterlynCore> easterlynProvider =
        chat.getServer().getServicesManager().getRegistration(EasterlynCore.class);
    if (easterlynProvider != null) {
      ConfigurationSection userSection = chat.getConfig().getConfigurationSection("auto_user");
      Map<String, String> userData = new HashMap<>();
      if (userSection != null) {
        userSection.getKeys(false).forEach(key -> userData.put(key, userSection.getString(key)));
      }
      Player player = event.getUser().getPlayer();
      if (player != null && !player.hasPlayedBefore()) {
        // TODO lang?
        new UserChatEvent(
            new AutoUser(easterlynProvider.getProvider(), userData),
            EasterlynChat.DEFAULT,
            event.getUser().getDisplayName() + " is new! Please welcome them.");
      }
    }

    addMainChannel(event.getUser(), null);
  }

  private void addMainChannel(@NotNull User user, @Nullable Collection<String> channels) {
    String channelName = user.getStorage().getString(EasterlynChat.USER_CURRENT);
    Channel current = channelName == null ? null : chat.getChannels().get(channelName);
    if (current == null
        && (channels == null
            || channels.isEmpty()
            || channels.contains(EasterlynChat.DEFAULT.getName()))) {
      user.getStorage().set(EasterlynChat.USER_CURRENT, EasterlynChat.DEFAULT.getName());
    }

    if (channels != null && !channels.isEmpty()) {
      return;
    }

    user.getStorage()
        .set(
            EasterlynChat.USER_CHANNELS,
            Collections.singletonList(EasterlynChat.DEFAULT.getName()));
    EasterlynChat.DEFAULT.getMembers().add(user.getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerJoin(PlayerJoinEvent event) {
    RegisteredServiceProvider<EasterlynCore> easterlynProvider =
        chat.getServer().getServicesManager().getRegistration(EasterlynCore.class);
    if (easterlynProvider == null) {
      return;
    }

    EasterlynCore core = easterlynProvider.getProvider();
    User user = core.getUserManager().getUser(event.getPlayer().getUniqueId());
    event.getPlayer().setDisplayName(user.getDisplayName());

    List<String> savedChannels = user.getStorage().getStringList(EasterlynChat.USER_CHANNELS);
    List<String> channels =
        savedChannels.stream()
            .filter(
                channelName -> {
                  Channel channel = chat.getChannels().get(channelName);
                  if (channel == null) {
                    return false;
                  }
                  if (channel.isWhitelisted(user)) {
                    channel.getMembers().add(user.getUniqueId());
                    return true;
                  }
                  return false;
                })
            .collect(Collectors.toList());

    if (channels.size() != savedChannels.size()) {
      user.getStorage().set(EasterlynChat.USER_CHANNELS, channels);
      if (!channels.contains(user.getStorage().getString(EasterlynChat.USER_CURRENT))) {
        user.getStorage().set(EasterlynChat.USER_CURRENT, null);
      }
    }

    addMainChannel(user, channels);

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    String time = dateFormat.format(new Date());

    chat.getServer()
        .getOnlinePlayers()
        .forEach(
            player -> {
              User otherUser;
              if (player.getUniqueId().equals(user.getUniqueId())) {
                otherUser = user;
              } else {
                otherUser = core.getUserManager().getUser(player.getUniqueId());
              }

              BaseComponent component = new TextComponent();
              component.addExtra(user.getMention());
              String locale = core.getLocaleManager().getLocale(player);
              for (TextComponent textComponent :
                  StringUtil.toJSON(core.getLocaleManager().getValue("chat.common.join", locale))) {
                component.addExtra(textComponent);
              }

              List<String> commonChannels =
                  otherUser.getStorage().getStringList(EasterlynChat.USER_CHANNELS);
              commonChannels.retainAll(channels);

              StringJoiner stringJoiner = new StringJoiner(", #", "#", " ").setEmptyValue("");
              for (String channel : commonChannels) {
                stringJoiner.add(channel);
              }
              String merged = stringJoiner.toString();

              if (commonChannels.size() > 1) {
                int lastComma = merged.lastIndexOf(',');
                int firstSegmentIndex = commonChannels.size() > 2 ? lastComma + 1 : lastComma;
                merged =
                    merged.substring(0, firstSegmentIndex)
                        + core.getLocaleManager().getValue("chat.common.and", locale)
                        + merged.substring(lastComma + 1);
              }
              for (TextComponent textComponent : StringUtil.toJSON(merged)) {
                if (!textComponent.getText().isEmpty()) {
                  component.addExtra(textComponent);
                }
              }
              for (TextComponent textComponent :
                  StringUtil.toJSON(
                      core.getLocaleManager().getValue("chat.common.at", locale, "{time}", time))) {
                component.addExtra(textComponent);
              }

              otherUser.sendMessage(component);
            });

    event.setJoinMessage("");
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    RegisteredServiceProvider<EasterlynCore> easterlynProvider =
        chat.getServer().getServicesManager().getRegistration(EasterlynCore.class);
    if (easterlynProvider == null) {
      return;
    }

    User user =
        easterlynProvider.getProvider().getUserManager().getUser(event.getPlayer().getUniqueId());
    List<String> savedChannels = user.getStorage().getStringList(EasterlynChat.USER_CHANNELS);

    List<String> channels =
        savedChannels.stream()
            .filter(
                channelName -> {
                  Channel channel = chat.getChannels().get(channelName);
                  if (channel == null) {
                    return false;
                  }
                  channel.getMembers().remove(user.getUniqueId());
                  return true;
                })
            .collect(Collectors.toList());

    if (channels.size() != savedChannels.size()) {
      user.getStorage().set(EasterlynChat.USER_CHANNELS, channels);
      if (!channels.contains(user.getStorage().getString(EasterlynChat.USER_CURRENT))) {
        user.getStorage().set(EasterlynChat.USER_CURRENT, null);
      }
    }
  }
}
