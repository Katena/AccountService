package com.splat;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Server implements AccountService {
    private Map<Integer, Long> cache = new HashMap<>();
    private Map<Integer, ReentrantReadWriteLock> locks = new HashMap<>();
    private static final String IP = "localhost";
    private static final String PORT = "3306";

    private Connection connection = null;

    public Server() {
        connectBD("root", "root");
    }

    private void connectBD(String userName, String userPass) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + IP + ":" + PORT + "/splat",
                    userName, userPass);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private ResultSet executeQuery(String query) {
        try {
            Statement stmt = connection.createStatement();

            boolean status = stmt.execute(query);
            if (status) {
                return stmt.getResultSet();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long getValueById(int id) throws SQLException {
        String query = "SELECT * FROM store WHERE id ='" + id + "'";
        ResultSet rs = executeQuery(query);

        if (rs.next()) {
            return rs.getLong("value");
        }
        return null;
    }

    private void insertValue(int id, Long value) throws SQLException {
        String query = "INSERT INTO store" + "(id, value) VALUES ('" + id + "','" + value + "')";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(query);
    }

    private void updateValue(int id, Long value) throws SQLException {
        String query = "UPDATE store SET value = '" + value + "' WHERE id ='" + id + "'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(query);
    }


    private void insertTimestamp(int id) throws SQLException {
        String query = "INSERT INTO callsfunctions" + "(functionsId, timestamp) VALUES ('" + id + "','" +
                new Timestamp(new java.util.Date().getTime()) + "')";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(query);
    }

    public Integer getCountRequestsAll(int id) throws SQLException {
        ResultSet rs = executeQuery("SELECT COUNT(*) FROM callsfunctions WHERE functionsId ='" + id + "'");

        if (rs.next()) {
            return rs.getInt(1);
        }
        return null;
    }

    public Integer getCountRequestsPerInterval(int id, int sec) throws SQLException {
        Timestamp ts = new Timestamp(new java.util.Date().getTime() - sec * 1000L);
        String query = "SELECT COUNT(*) FROM callsfunctions WHERE functionsId  ='" + id +
                "' AND timestamp >='" + ts + "'";

        ResultSet rs = executeQuery(query);

        if (rs.next()) {
            return rs.getInt(1);
        }
        return null;
    }


    public void clearLogs() throws SQLException {
        String query = "TRUNCATE TABLE callsfunctions";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(query);
    }

    @Override
    public Long getAmount(Integer id) throws SQLException {
        insertTimestamp(1);
        if (!locks.containsKey(id)) {
            locks.put(id, new ReentrantReadWriteLock());
        }
        locks.get(id).readLock().lock();

        try {
            if (cache.containsKey(id)) {
                return cache.get(id);
            }

            Long retValue = getValueById(id);
            if (retValue != null) {
                cache.put(id, retValue);
            } else {
                retValue = 0L;
            }
            return retValue;
        } finally {
            locks.get(id).readLock().unlock();
        }
    }

    @Override
    public void addAmount(Integer id, Long value) throws SQLException {
        insertTimestamp(2);
        if (!locks.containsKey(id)) {
            locks.put(id, new ReentrantReadWriteLock());
        }
        locks.get(id).writeLock().lock();

        try {
            Long newValue = getValueById(id);
            if (newValue == null) {
                newValue = 0L;
                insertValue(id, 0L);
            }
            newValue += value;

            updateValue(id, newValue);
            cache.put(id, newValue);

        } finally {
            locks.get(id).writeLock().unlock();
        }
    }

    public String toString() {
        return cache.toString();
    }

    public static void main(String args[]) {
        LogGUI gui = new LogGUI();
        gui.log(3);
        try {
            Server obj = new Server();
            AccountService stub = (AccountService) UnicastRemoteObject.exportObject(obj, 0);
            Registry registry = LocateRegistry.createRegistry(777);
            registry.bind("AccountService", stub);

            obj.clearLogs();

            System.out.println("Server ready");
            while (true) {
                Thread.sleep(1000);

                int countGetAmount = obj.getCountRequestsPerInterval(1, 1);
                int countAddAmount = obj.getCountRequestsPerInterval(2, 1);
                gui.logIntervalPrint(countGetAmount, countAddAmount);
                gui.lastSecPrint(countGetAmount, countAddAmount);

                int cntGetAmountTotal = obj.getCountRequestsAll(1);
                int cntAddAmountTotal = obj.getCountRequestsAll(2);
                gui.logTotalPrint(cntGetAmountTotal, cntAddAmountTotal);
            }
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
