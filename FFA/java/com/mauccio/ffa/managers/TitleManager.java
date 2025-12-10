package com.mauccio.ffa.managers;

import com.mauccio.ffa.Core;
import com.mauccio.ffa.util.Titles;
import org.bukkit.entity.Player;

public class TitleManager {

    private final Core core;

    private class Title {

        String count10;
        String count5;
        String count4;
        String count3;
        String count2;
        String count1;
        String gameStarted;

        public Title() {

            count10 = core.getLangManager().getText("titles.countdown.ten-seconds");
            count5 = core.getLangManager().getText("titles.countdown.five-seconds");
            count4 = core.getLangManager().getText("titles.countdown.four-seconds");
            count3 = core.getLangManager().getText("titles.countdown.three-seconds");
            count2 = core.getLangManager().getText("titles.countdown.two-seconds");
            count1 = core.getLangManager().getText("titles.countdown.one-second");
            gameStarted = core.getLangManager().getText("titles.game-started");
        }

        public String getCount10() {
            return count10;
        }

        public String getCount5() {
            return count5;
        }

        public String getCount4() {
            return count4;
        }

        public String getCount3() {
            return count3;
        }

        public String getCount2() {
            return count2;
        }

        public String getCount1() {
            return count1;
        }

        public String getGameStarted() {
            return gameStarted;
        }
    }

    private class SubTitle {

        String count10;
        String count5;
        String count4;
        String count3;
        String count2;
        String count1;
        String gameStarted;

        public SubTitle() {

            count10 = core.getLangManager().getText("subtitles.countdown.ten-seconds");
            count5 = core.getLangManager().getText("subtitles.countdown.five-seconds");
            count4 = core.getLangManager().getText("subtitles.countdown.four-seconds");
            count3 = core.getLangManager().getText("subtitles.countdown.three-seconds");
            count2 = core.getLangManager().getText("subtitles.countdown.two-seconds");
            count1 = core.getLangManager().getText("subtitles.countdown.one-second");
            gameStarted = core.getLangManager().getText("subtitles.game-started");
        }

        public String getCount10() {
            return count10;
        }

        public String getCount5() {
            return count5;
        }

        public String getCount4() {
            return count4;
        }

        public String getCount3() {
            return count3;
        }

        public String getCount2() {
            return count2;
        }

        public String getCount1() {
            return count1;
        }

        public String getGameStarted() {
            return gameStarted;
        }
    }

    public TitleManager(Core core) {
        this.core = core;
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Titles.sendFullTitle(player,
                fadeIn,
                stay,
                fadeOut,
                title,
                subtitle);
    }

    public void sendCountdown10(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount10(),
                subTitle.getCount10(),
                0,
                30,
                0);
    }

    public void sendCountdown5(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount5(),
                subTitle.getCount5(),
                0,
                30,
                0);
    }

    public void sendCountdown4(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount4(),
                subTitle.getCount4(),
                0,
                30,
                0);
    }

    public void sendCountdown3(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount3(),
                subTitle.getCount3(),
                0,
                30,
                0);
    }

    public void sendCountdown2(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount2(),
                subTitle.getCount2(),
                0,
                30,
                0);
    }

    public void sendCountdown1(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getCount1(),
                subTitle.getCount1(),
                0,
                30,
                0);
    }

    public void sendGameStarted(Player player) {
        Title title = new Title();
        SubTitle subTitle = new SubTitle();
        sendTitle(player,
                title.getGameStarted(),
                subTitle.getGameStarted(),
                0,
                30,
                0);
    }
}