import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

ListId:{
	if (m.getHeader("List-Id") != null && m.getHeader("List-Id").length == 1) {
		Pattern p = ~/<([a-zA-Z\-]*)\..*>"/;
		String listId = m.getHeader("List-Id")[0];
		Matcher r = p.matcher(listId);
		if (r.matches()) {
			listId = r.group(1);
	
			if (listId.startsWith("all-")) {
				listId = "all";
			}
			target = listId;
		}
	}
}

SvnCommit:{
	Pattern p = ~/\[svn-commit\] \/company\/([a-zA-Z0-9_\/]*)/;
	Matcher r = p.matcher(m.getSubject());
	if (r.matches()) {
		target = "svn/" + r.group(1).replaceAll("/", "-");
	}
}

Hudson:{
	Pattern p = ~/(?:Hudson build is back to normal|Build failed in Hudson): ([a-zA-Z0-9]*).*/;
	Matcher r = p.matcher(m.getSubject());
	if (r.matches()) {
		target = "hudson/" + r.group(1);
	}
}

Bugzilla:{
	InternetAddress bugzilla = new InternetAddress("buggy@example.com", true);
	if (m.getFrom()[0].equals(bugzilla)) {
		target = "bugzilla";
	
		if (m.getHeader("X-Bugzilla-Product") != null && m.getHeader("X-Bugzilla-Product").length == 1) {
			String product = m.getHeader("X-Bugzilla-Product")[0];
			product = product.replaceAll(" ", "-").toLowerCase();
			target += "/" + product;
		}
	
		if (m.getHeader("X-Bugzilla-Component") != null && m.getHeader("X-Bugzilla-Component").length == 1) {
			String product = m.getHeader("X-Bugzilla-Component")[0];
			product = product.replaceAll(" ", "-").toLowerCase();
			target += "/" + product;
		}
	}
}

Foogor:{
	Pattern p = ~/foogor rule change: ([a-zA-Z0-9_]*).*/;
	Matcher r = p.matcher(m.getSubject());
	if (r.matches()) {
		target = "foo/" + r.group(1);
	}
}

BarInstall:{
	Pattern p = ~/BAR_INSTALL: ([a-zA-Z0-9]*).*/;
	Matcher r = p.matcher(m.getSubject());
	if (r.matches()) {
		target = "bar/" + r.group(1);
	}
}

Spam:{
	Pattern p = ~/([a-zA-Z]*)\..*/
	if (m.getHeader("X-Spam-Detected") != null ||
		(m.getHeader("X-Spam-Status") != null &&
		m.getHeader("X-Spam-Status").length == 1 &&
		m.getHeader("X-Spam-Status")[0].equals("Yes"))) {
		target = "Junk";
	}
}

InternetAddress me = new InternetAddress("username@example.com", true);
for (Address a : m.getRecipients(RecipientType.TO)) {
	if (a.equals(me)) {
		target = null;
	}
}

target = "INBOX/" + target;
