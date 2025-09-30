package audio;

import java.lang.reflect.Constructor;
import java.util.HashMap;


import org.tinylog.Logger;

public class Terminal {	
	static { // needs to be positioned as first entry in class!
		System.setProperty("java.awt.headless", "true");
	}	

	@FunctionalInterface
	interface CommandProvider {
		Command create();
	}

	static class CommandClassProvider implements CommandProvider {
		private Constructor<? extends Command> commandClassConstructor;

		public CommandClassProvider(Class<? extends Command> commandClass) {
			try {
				this.commandClassConstructor = commandClass.getDeclaredConstructor();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Command create() {
			try {
				return commandClassConstructor.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}
	}

	static class CommandArgsProvider implements CommandProvider {
		private final CommandArgs commandArgs;
		
		public CommandArgsProvider(CommandArgs commandArgs) {
			this.commandArgs = commandArgs;
		}
		
		@Override
		public Command create() {
			return commandArgs;			
		}
	}

	private static HashMap<String, CommandProvider> commandMap = new HashMap<String, CommandProvider>();
	static {
		putArgsCommand("server", audio.server.Webserver::main);
	}

	private static void putClassCommand(String name, Class<? extends Command> commandClass) {
		try {
			CommandClassProvider commandProvider = new CommandClassProvider(commandClass);			
			commandMap.put(name, commandProvider);
		} catch(Exception e) {
			Logger.warn(e);
		}
	}

	private static void putArgsCommand(String name, CommandArgs commandArgs) {
		try {
			CommandArgsProvider commandArgsProvider = new CommandArgsProvider(commandArgs);			
			commandMap.put(name, commandArgsProvider);
		} catch(Exception e) {
			Logger.warn(e);
		}
	}

	private static String[] copyStrings(String[] original, int start) {
		int newLength = original.length - start;
		String[] copy = new String[newLength];
		System.arraycopy(original, start, copy, 0, newLength);
		return copy;
	}

	public static void main(String[] args) throws Exception {
		if(args.length == 0) {
			audio.server.Webserver.main(args);
		} if(args.length > 0) {
			String command = args[0];
			String[] params = copyStrings(args, 1);
			command(command, params);			
		} else {
			throw new RuntimeException("invalid command");
		}
	}

	public static void command(String command, String[] params) {
		CommandProvider commandProvider = commandMap.get(command);
		if(commandProvider != null) {
			try {
				Command commandObject = commandProvider.create(); 
				commandObject.execute(command, params);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("command not found [" + command + "]");
		}	
	}
}
