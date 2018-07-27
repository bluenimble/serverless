package helpers;
 
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
 
public class DefaultCustomizer implements SessionCustomizer {
 
	@Override
	public void customize (Session session) throws Exception {
		session.getLogin ().addSequence (new LongUUIDSequence ("long-uuid"));
		session.getLogin ().addSequence (new StringUUIDSequence ("string-uuid"));
	}
 
}