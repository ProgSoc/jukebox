package locks;

/**
 * An interface that describes a class that can be locked (made immutable) for
 * as long as necessary.
 * 
 * @author Jableader
 */
public interface Lockable {
	public Key lock();

	public void unlock(Key k);

	public boolean isLocked();
}
