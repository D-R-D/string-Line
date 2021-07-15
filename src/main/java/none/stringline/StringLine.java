package none.stringline;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class StringLine extends JavaPlugin implements Listener
{
    //タイマーの生死確認用の真偽値
    boolean Boolean = false , savlean = false , firlean = false;
    String name;
    //true = living , false = dead

    //
    /*プラグイン起動時に呼び出されるイベント*/
    //
    @Override
    public void onEnable()
    {
        //イベントハンドラを有効化する
        getServer().getPluginManager().registerEvents(this,this);

        String names = null;
        try{
        String pass = System.getProperty("user.dir");
        File file = new File(pass + "/plugins/name/name.txt");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        name = br.readLine();
        names = ("alert server started [ "+name+" ]");
        br.close();
        }
        catch(FileNotFoundException e){ System.out.println(e.getMessage()); }
        catch (IOException e) { e.printStackTrace(); }
        eventsender(name+":started");
        sinssender(names);


        //senderでbotに通知、getLoggerでログを打つ

        /*
        Thread r_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiver();
            }
        });
        r_thread.start();
         */

        //sender("status:true");
        //sender("[plugin]:サーバーが開きました。");
        //sender("[plugin]:プレイヤーのログインを待っています。ログインがない場合、10分後にサーバーをシャットダウンします。");
        getLogger().info("タイマーを開始します。");

        //自動シャットダウン用のタイマーを起動、生死確認用の真偽値を真にしておく
        timers();
        //sub_timers();
        Boolean = true;
    }
    //
    /*プラグイン起動時に呼び出されるイベント*/
    //

    //
    /*プラグイン終了時に呼び出されるイベント*/
    //現在botへの通知のみ
    @Override
    public void onDisable()
    {
        String named = "container:dead:"+name;
        eventsender(name+":stopped");
        sinssender(named);
        //sender("status:false");
        //sender("[plugin]:サーバーが終了しました。");
        //sender("[plugin]:サーバーの再起動には、コマンド/startを使用してください。");
    }
    //
    /*プラグイン終了時に呼び出されるイベント*/
    //

    //
    /*プレイヤーがログアウトした時に呼び出されるイベント*/
    //
    @EventHandler
    public void onLogoutEvent(PlayerQuitEvent event) {
        //botへ通知(邪魔だったらここを消す)
        //sender("[plugin]:プレイヤーがログアウトしました。");

        //オンラインのプレイヤーが0になったらセーブ かつ timerが死んでるときに新しくtimerを起動する。
        //生死判定用真偽値の更新を忘れずに
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    getLogger().info("ワールドのセーブを開始します。");
                    sinssender("alert container_name " + name + " : プレイヤー数が0になりました。ワールドのセーブを開始します。");
                    Save();
                    if (!Boolean) {
                        Boolean = true;
                        //sender("[plugin]:オンラインプレイヤーが0です。シャットダウンを開始します。");
                        //sender("[plugin]:サーバーのシャットダウンを開始しています。10分後にサーバーをシャットダウンします。");
                        sinssender("alert container_name " + name + " : 新しくログインがない場合10分後にシャットダウンされます。");
                        getLogger().info("タイマーを開始します。");
                        timers();
                    }
                }
            }
    }, 20);
    }
    //
    /*プレイヤーがログアウトした時に呼び出されるイベント*/
    //

    //
    /*プレイヤーがログインした時に呼び出されるイベント*/
    //
    @EventHandler
    public void onLoginEvent(PlayerJoinEvent event)
    {
        //既に1人以上ログインしているため生死確認用真偽値の確認だけでok
        //実際にtimerを停止する部分はtimerに組み込み済みだから気にしないで

        firlean = true;
        savlean = true;

        if (Boolean)
        {
            getLogger().info("サーバーのシャットダウンを停止します。");
        }
    }
    //
    /*プレイヤーがログインした時に呼び出されるイベント*/
    //

    //
    /*セーブ*/
    //
    private void Save()
    {
        getLogger().info("サーバーをセーブ中です…");
        savePlayers();
        saveWorld();
        getLogger().info("サーバーのセーブが終了しました。");
        sinssender("alert container_name " + name +" : セーブが完了しました。");
    }

    private void savePlayers()
    {
        this.getServer().savePlayers();
    }

    private void saveWorld()
    {
        List<World> worlds = this.getServer().getWorlds();
        for (World world : worlds) {
            world.save();
        }
    }
    //
    /**/
    //

    //
    /*自動シャットダウン用タイマー*/
    //
    public void timers() {
        //バグ回避のためタイマーは毎回新しく作る
        Timer timer = new Timer();

        //タイマーで回すタスク
        TimerTask task = new TimerTask() {
            //初回だけ実行される部分
            int i = 0;

            //
            /*タイマーで毎回回される部分*/
            //
            @Override
            public void run() {
                i++;
                //オンラインのプレイヤー数が0以上の時タイマーを終了させる
                //生死確認用真偽値の更新を忘れずに
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    getLogger().info("[plugin.timers]:サーバーのシャットダウンを停止しました。");
                    Boolean = false;
                    timer.cancel();
                    timer.purge();
                    return;
                }

                //10回目のループ(10分後)にサーバーをシャットダウンする
                //生死確認用真偽値の更新を忘れずに
                if (i == 10) {
                    getLogger().info("[plugin.timers]:10分経過しました。サーバーをシャットダウンします。");
                    getServer().shutdown();
                    Boolean = false;
                    timer.cancel();
                    timer.purge();
                    return;
                }
                getLogger().info("[plugin.timers]:" + i + "分経過しました。");
            }
            //
            /*タイマーで毎回回される部分*/
            //
        };

        //タイマーの設定
        timer.scheduleAtFixedRate(task, 60000, 60000);
        //タイマーの設定
    }

    /*
    public void sub_timers() {
        //バグ回避のためタイマーは毎回新しく作る
        Timer timer = new Timer();
        //タイマーで回すタスク
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty() && firlean)
                {
                    if(savlean) {
                        savlean = false;
                        getLogger().info("[plugin]:ワールドのセーブを開始します。");
                        sinssender("alert container_name " + name + " : プレイヤー数が0になりました。ワールドのセーブを開始します。");
                        Save();
                    }
                    if(!Boolean) {
                        Boolean = true;
                        sinssender("alert container_name " + name + " : 新しくログインがない場合10分後にシャットダウンされます。");
                        getLogger().info("[plugin]:タイマーを開始します。");
                        timers();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(task, 1000, 1000);
    }
    */

    //
    /*botにudp通信でデータを渡す*/
    //
    /*
    public void sender(String str)
    {
        //strをbyte配列に変換
        byte[] data;
        data = str.getBytes(StandardCharsets.UTF_8);
        //接続用のソケットを作成
        DatagramSocket sock = null;
        try { sock = new DatagramSocket(); }
        catch (SocketException e) { e.printStackTrace();}
        //パケットを作成し、udpで送信する。
        DatagramPacket packet = new DatagramPacket(data,data.length,new InetSocketAddress("127.0.0.1",6001));
        try { sock.send(packet); }
        catch (IOException e) { e.printStackTrace(); }
        //最後にお片付けして終了
        sock.close();
    }
    */
    //
    /*botにudp通信でデータを渡す*/
    //


    //
    /*sinsにudp送信*/
    //
    public void sinssender(String str)
    {
        //strをbyte配列に変換
        byte[] data;
        data = str.getBytes(StandardCharsets.UTF_8);
        //接続用のソケットを作成
        DatagramSocket sock = null;
        try { sock = new DatagramSocket(); }
        catch (SocketException e) { e.printStackTrace();}
        //パケットを作成し、udpで送信する。
        DatagramPacket packet = new DatagramPacket(data,data.length,new InetSocketAddress("127.0.0.1",6011));
        try { sock.send(packet); }
        catch (IOException e) { e.printStackTrace(); }
        //最後にお片付けして終了
        sock.close();

        getLogger().info("udp sended");
    }

    public void eventsender(String str)
    {
        //strをbyte配列に変換
        byte[] data;
        data = str.getBytes(StandardCharsets.UTF_8);
        //接続用のソケットを作成
        DatagramSocket sock = null;
        try { sock = new DatagramSocket(); }
        catch (SocketException e) { e.printStackTrace();}
        //パケットを作成し、udpで送信する。
        DatagramPacket packet = new DatagramPacket(data,data.length,new InetSocketAddress("127.0.0.1",7011));
        try { sock.send(packet); }
        catch (IOException e) { e.printStackTrace(); }
        //最後にお片付けして終了
        sock.close();

        getLogger().info("event sended");
    }
    //
    /*sinsにudp送信*/
    //


    //
    /*sinsからデータを受け取る*/
    //
    public void receiver() {
        //データ受信
        byte[] data = new byte[1024];
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket(6011);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        DatagramPacket packet = new DatagramPacket(data, data.length);

        while (true) {
            try {
                sock.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String str = null;
            try {
                str = new String(Arrays.copyOf(packet.getData(), packet.getLength()), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            getLogger().info(str);

            //データごとに処理
            if(str.equals("stop"))
            {
                getServer().shutdown();
            }
        }
    }
    //
    /*sinsからデータを受け取る*/
    //
}
