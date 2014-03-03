package net.aerenserve.networkpoints;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.aerenserve.minesql.MineSQL;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class NetworkPoints extends JavaPlugin implements Listener {
	
	MineSQL minesql;
	
	@Override
	public void onEnable() {
		
		saveDefaultConfig();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		String host = getConfig().getString("database.ip");
		String port = getConfig().getString("database.port");
		String database = getConfig().getString("database.dbname");
		String user = getConfig().getString("database.user");
		String pass = getConfig().getString("database.pass");
		
		minesql = new MineSQL(this, host, port, database, user, pass);
		
		try {
			minesql.updateSQL("CREATE TABLE IF NOT EXISTS `playerpoints` (id int PRIMARY KEY AUTO_INCREMENT, username text, balance int);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		getLogger().info("NetworkPoints v1.3 by hatten33 enabled");
	}
	
	@Override
	public void onDisable() {
		getLogger().info("NetworkPoints v1.3 by hatten33 disabled");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equalsIgnoreCase("mypoints")) {
				p.sendMessage(ChatColor.GRAY + "You currently have " + ChatColor.GREEN + getBalance(p.getName()) + ChatColor.GRAY + " points.");
			}
		}
		return false;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		createPlayer(e.getPlayer().getName());
	}
	
	public void createPlayer(String playername) {
		if(!playerExists(playername)) {
			try {
				minesql.updateSQL("INSERT INTO playerpoints (username, balance) VALUES ('" + playername + "',0);");
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public boolean playerExists(String playername) {
		try {
			ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + playername + "';");
			if(res.next()) {
				if(res.getString("username") == null) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return false;
	}
	
	public boolean checkTransaction(String playername, Integer amount) {
		if(getBalance(playername) >= amount) return true;
		else return false;
	}
	
	public Integer getBalance(String playername) {
		Integer retval = null;
		try {
			ResultSet res = minesql.querySQL("SELECT * FROM playerpoints WHERE username = '" + playername + "';");
			if(res.next()) {
				if((Integer) res.getInt("balance") != null) {
					retval = res.getInt("balance");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retval;
	}
	
	public void addPoints(String playername, Integer amount) {
		if(playerExists(playername)) {
			setBalance(playername, (getBalance(playername) + amount));
		} else {
			createPlayer(playername);
			addPoints(playername, amount);  //This might be bad? time will tell
		}
	}
	
	public void setBalance(String playername, Integer amount) {
		if(playerExists(playername)) {
			try {
				minesql.updateSQL("UPDATE playerpoints SET balance=" + amount + " WHERE username='" + playername + "';");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			createPlayer(playername);
			setBalance(playername, amount);
		}
	}
	
	public void removePoints(String playername, Integer amount) { //TODO add special exceptions
		if(playerExists(playername)) {
			if(checkTransaction(playername, amount)) {
				setBalance(playername, (getBalance(playername) - amount));
			}
		} else {
			createPlayer(playername);
		}
	}
}
