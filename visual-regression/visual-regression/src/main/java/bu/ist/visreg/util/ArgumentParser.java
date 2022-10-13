package bu.ist.visreg.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for identifying named arguments out of a series of positional arguments, where those
 * positional arguments that are hyphenated indicate the name of the following argument.
 * 	Example:
 * 		java ArgumentParser --argname1 argvalue1 -b argvalue2 argvalue3
 * 		argvalue1 and argvalue2 are both named arguments, and argvalue3 is an unnamed argument.
 * 
 * @author wrh
 *
 */
public class ArgumentParser {

	Map<String, String> namedArgs = new HashMap<String, String>();
	List<String> unamedArgs = new ArrayList<String>();
	
	public ArgumentParser(String[] args) throws IllegalArgumentException {
		String key = null;
		for(int i=0; i<args.length; i++) {
			if(args[i].startsWith("-")) {
				key = args[i].replaceFirst("\\-+", "").toLowerCase();
				if(i==(args.length-1)) {
					throw new IllegalArgumentException("Invalid argument: " + key + " has no value.");
				}
				else {
					namedArgs.put(key, args[++i]);
				}
			}
			else {
				unamedArgs.add(args[i]);
			}
		}
	}
	
	public int getInteger(String key) {
		return Integer.parseInt(getString(key));
	}
	
	public String getString(String key) {
		return find(key);
	}
	
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(getString(key));
	}
	
	public String getPositionalArg() {
		if(unamedArgs.isEmpty()) return null;
		return unamedArgs.get(0);
	}
	
	public List<String> getPositionalArgs() {
		return unamedArgs;
	}
	
	private String formatKey(String key) {
		if(key.startsWith("-")) {
			return key.replaceFirst("\\-+", "").toLowerCase();
		}
		return key.toLowerCase();
	}

	private String find(String key) {
		String[] keys = key.split("\\|");
		String key1 = formatKey(keys[0]);
		String key2 = null;
		if(keys.length > 1) {
			key2 = formatKey(keys[1]);
		}
		
		if(namedArgs.containsKey(key1)) {
			if(key2 != null && !namedArgs.containsKey(key2)) {
				namedArgs.put(key2, namedArgs.get(key1));
			}
			return namedArgs.get(key1);
		}
		else if(key2 != null && namedArgs.containsKey(key2)) {
			if(!namedArgs.containsKey(key1)) {
				namedArgs.put(key1, namedArgs.get(key2));
			}
			return namedArgs.get(key2);
		}
		return null;
	}
	
	/**
	 * Check if each of a number or arguments were parsed by checking for them with the specified keys.
	 * The keys can each be a simple string, or a string expression that indicates either of two values (separated by a "|" character).
	 * @param keyCombo
	 * @return
	 */
	public boolean has(String... keyCombo) {
		for(String key : keyCombo) {
			if(!has(key)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check if the an argument was parsed by checking for it by key.
	 * The key can be a simple string, or a string expression that indicates either of two values (separated by a "|" character).
	 * @param key
	 * @return
	 */
	public boolean has(String key) {
		return find(key) != null;
	}
		
	@Override
	public String toString() {
		return "ArgumentParser [namedArgs=" + namedArgs + ", unamedArgs=" + unamedArgs + "]";
	}

	public static void main(String[] args) {
		ArgumentParser parser = new ArgumentParser(new String[]{"arg1", "-k", "arg2", "--key2", "arg3", "arg4", "--key3", "8", "--KEY4", "True"});
		System.out.println(parser);
		System.out.println(parser.getString("k"));
		System.out.println(parser.getString("--key2"));
		System.out.println(parser.getInteger("key3"));
		System.out.println(parser.getPositionalArg());
		System.out.println(parser.getPositionalArgs());
		System.out.println(parser);
		System.out.println(parser.getString("k|key1"));
		System.out.println(parser);
		System.out.println(parser.getBoolean("bogusKey"));
		System.out.println(parser.getBoolean("Key4"));
		System.out.println(parser.has("key2", "key3"));
		System.out.println(parser.has("k2|key2", "k3|key3"));
		System.out.println(parser.has("key2", "key5"));
		
	}
}
