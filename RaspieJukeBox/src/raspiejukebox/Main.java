package raspiejukebox;

import java.lang.reflect.Method;
import java.util.Scanner;

/**
 * The main non web interface, used mainly (if not entirely) for testing. First
 * it will try to check if its a preset function, eg exit. Then it will go
 * Reflectively calling the given function on first the jukebox. If the function
 * does not exist on the jukebox it tries the database. It accepts arguements
 * (string, int, float or null) through a > operator. There is no nesting.<br>
 * 
 * For example:<br>
 * getTrackById > 5<br>
 * play<br>
 * unlock > null<br>
 * 
 * someFunction > arg1 > arg2 > ...
 * 
 * @author Jableader
 */
public class Main {
	
	public static Wrapper getMethod(String name) {
		
		Method m;
		for (Object o: new Object[]{JukeBox.get(), TracksDatabase.get()})
			if ((m = getMethod(o, name)) != null)
				return new Wrapper(o, m);
		
		if (name.matches("[sg]etVol")){
			try{
				
			} catch (Exception e){
				System.out.println("[sg]etVol Error: " + e.getMessage());
			}
		}
		
		return null;
	}
	
	public static Method getMethod(Object o, String name) {

		Method[] methods = o.getClass().getMethods();
		for (Method m : methods)
			if (m.getName().equals(name))
				return m;
		return null;
	}

	public static void main(final String[] args) {
		final TracksDatabase td = TracksDatabase.get();

		final JukeBox jb = JukeBox.get();

		if (args != null && args.length > 0 && args[0].equals("true"))
			HardwareInterface.initialise();

		System.out.println("Ready");

		Scanner sc = new Scanner(System.in);

		while (true) {
			Expression ex = Expression.evaluate(sc.nextLine());
			System.out.println(ex.toString());
			
			if (ex.name.equals("exit")) {
				jb.stop();
				System.exit(0);
			}
			Wrapper w = getMethod(ex.name);
			
			if (w != null)
				try {
					Object res = w.method.invoke(w.obj, ex.args);

					System.out
							.println("==> " + java.util.Objects.toString(res));
					if (res instanceof Iterable)
						for (Object j : (Iterable) res)
							System.out.println("\t" + j.toString());

				} catch (Exception exc) {
					System.out.println(exc.getMessage());
				}
			else
				System.out.println("No such method");
		}
	}

	static class Expression {
		public String name = "";
		public Object[] args = {};

		private static String ptnInt = "^[0-9]+$";
		private static String ptnFloat = "[0-9]+\\.[0-9]+";
		private static String ptnString = "^\".*?[^\\\\]\"";

		private Expression(String name, Object[] args) {
			this.name = name;
			this.args = args;
		}

		public static Expression evaluate(String s) {
			try {
				String[] ss = s.split("\\s*>\\s*");
				String name = ss[0];

				Object[] args = new Object[ss.length - 1];

				for (int j = 1; j < ss.length; j++)
					if (ss[j].matches(ptnString))
						args[j - 1] = ss[j];
					else if (ss[j].matches(ptnInt))
						args[j - 1] = Integer.parseInt(ss[j]);
					else if (ss[j].matches(ptnFloat))
						args[j - 1] = Float.parseFloat(ss[j]);
					else if (ss[j].equals("null"))
						args[j - 1] = null;
					else
						args[j - 1] = ss[j];

				return new Expression(name.trim(), args);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("Cannot parse expression");
				return new Expression("", new Object[0]);
			}
		}
		
		public String toString(){
			String s = name + "(";
			String prefix = "";
			for (Object o: args){
				s += prefix + o.getClass().toString() + ": " + o.toString();
				prefix = ", ";
			}
			return s + ")";
		}
	}
	
	static class Wrapper {
		public final Method method;
		public final Object obj;
		
		Wrapper(Object o, Method m){
			method = m;
			obj = o;
		}
	}
}