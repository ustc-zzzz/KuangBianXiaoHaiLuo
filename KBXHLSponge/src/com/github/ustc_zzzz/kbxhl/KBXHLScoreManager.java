package com.github.ustc_zzzz.kbxhl;

import org.spongepowered.api.boss.*;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * @author ustc_zzzz
 */
public class KBXHLScoreManager
{
    static final float MAX_SCORE = 651.0F;

    private final KBXHLSponge plugin;

    private final Map<Player, Integer> scores = new WeakHashMap<>();
    private final Map<Player, Instant> startTime = new WeakHashMap<>();
    private final Map<Player, ServerBossBar> bossbars = new WeakHashMap<>();
    private final Map<Player, Integer> scoresToBeAdded = new WeakHashMap<>();

    KBXHLScoreManager(KBXHLSponge plugin)
    {
        this.plugin = plugin;
    }

    void init()
    {
        Task.builder().intervalTicks(1).execute(this::tick).submit(this.plugin);
    }

    private ServerBossBar createBossBarFor(Player k)
    {
        BossBarColor color = BossBarColors.PURPLE;
        BossBarOverlay overlay = BossBarOverlays.PROGRESS;
        Text name = Text.of(TextColors.GOLD, TextStyles.BOLD, "狂扁小海螺");
        return ServerBossBar.builder().name(name).color(color).overlay(overlay).build().addPlayer(k);
    }

    private void tick(Task task)
    {
        for (Map.Entry<Player, Integer> entry : scoresToBeAdded.entrySet())
        {
            Integer oldScore = entry.getValue();
            Player player = entry.getKey();

            int value = Objects.requireNonNull(scoresToBeAdded.computeIfPresent(player, (k, v) -> Math.max(v - 5, 0)));
            int score = scores.getOrDefault(player, 0) + oldScore - value;

            bossbars.computeIfPresent(player, (k, v) -> v.setPercent(Math.min(score / MAX_SCORE, 1.0F)));
            scores.put(player, score);
        }
    }

    public int add(Player player, int score)
    {
        scoresToBeAdded.compute(player, (k, v) -> Objects.nonNull(v) ? v + score : score);
        bossbars.computeIfAbsent(player, this::createBossBarFor);
        startTime.computeIfAbsent(player, k -> Instant.now());
        return scores.getOrDefault(player, 0);
    }

    public Duration remove(Player player)
    {
        scores.remove(player);
        scoresToBeAdded.remove(player);
        Optional.ofNullable(bossbars.remove(player)).ifPresent(v -> v.removePlayer(player));
        return Optional.ofNullable(startTime.remove(player)).map(v -> Duration.between(v, Instant.now())).orElse(Duration.ZERO);
    }
}
