package helpers;
 
import java.util.UUID;
import java.util.Vector;
 
import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sequencing.Sequence;
 
public class StringUUIDSequence extends Sequence {
 
	public StringUUIDSequence () {
		super ();
	}
 
	public StringUUIDSequence (String name) {
		super (name);
	}
 
	@Override
	public Object getGeneratedValue (Accessor accessor,
			AbstractSession writeSession, String seqName) {
		return UUID.randomUUID().toString().toUpperCase();
	}
 
	@Override
	public Vector getGeneratedVector(Accessor accessor,
			AbstractSession writeSession, String seqName, int size) {
		return null;
	}
 
	@Override
	public void onConnect() {
	}
 
	@Override
	public void onDisconnect() {
	}
 
	@Override
	public boolean shouldAcquireValueAfterInsert() {
		return false;
	}
 
	@Override
	public boolean shouldUseTransaction() {
		return false;
	}
 
	@Override
	public boolean shouldUsePreallocation() {
		return false;
	}
 
}