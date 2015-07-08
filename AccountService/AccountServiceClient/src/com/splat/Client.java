package com.splat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.*;


public class Client implements Runnable {
    private AccountService serv;
    private int type; // 0 - getAmount, 1 - addAmount
    private Random rand;
    private int countRequests;
    private static int rCount;
    private static int wCount;
    private static int[] idList;

    public Client() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(new File("config.ini")));

        rCount = Integer.valueOf(props.getProperty("rCount"));
        wCount = Integer.valueOf(props.getProperty("wCount"));

        String[] parts = props.getProperty("idList").split(";");
        idList = new int[parts.length];
        for (int i = 0; i < parts.length ; ++i) {
            idList[i] = Integer.valueOf(parts[i]);
        }
    }

    public Client(AccountService serv, int[] idList, int type, int countRequests) {
        this.serv = serv;
        this.idList = idList;
        this.type = type;
        this.countRequests = countRequests;
        rand = new Random();
    }

    @Override
    public void run() {
        int ind = rand.nextInt(idList.length);
        int id = idList[ind];
        try {
            for (int i = 0; i < countRequests; i++) {
                Thread.sleep(rand.nextInt(300));
                if (type == 0) { // getAmount
                    serv.getAmount(id);
                } else { // addAmount
                    int nv = rand.nextInt(10);
                    serv.addAmount(id, (long) nv);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] agrs) throws SQLException, IOException {
        new Client();
        System.out.println("Client connect");

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 777);
            AccountService serv = (AccountService) registry.lookup("AccountService");

            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < rCount; i++) {
                Client client = new Client(serv, idList, 0, 100);
                threads.add(new Thread(client));
            }
            for (int i = 0; i < wCount; i++) {
                Client client = new Client(serv, idList, 1, 100);
                threads.add(new Thread(client));
            }

            for (Thread t : threads) {
                t.start();
            }

            while (true) {
                Thread.sleep(1000);

                boolean alive = false;
                for (Thread t : threads) {
                    if (t.isAlive()) {
                        alive = true;
                        break;
                    }
                }
                if (!alive) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}



