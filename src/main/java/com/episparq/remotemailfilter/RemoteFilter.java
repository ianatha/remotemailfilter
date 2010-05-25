package com.episparq.remotemailfilter;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.search.FlagTerm;
import javax.security.auth.x500.X500Principal;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

public class RemoteFilter extends Thread {
	// http://svn.assembla.com/svn/SampleCode/gep/src/gep/minmax/UpsideDownShell.groovy
	/*
	 * public static Reader getScript() { try { return new FileReader(new
	 * File("/Users/thatha/a.groovy")); } catch (Exception e) { return null; } }
	 */

	private IMAPStore store;

	public RemoteFilter(String rules, String hostname, String user,
			String password) throws RemoteFilterException {
		this(rules, hostname, 143, user, password);
	}

	public RemoteFilter(String rules, String hostname, int port,
			String user, String password) throws RemoteFilterException {
		this(rules, hostname, port, user, password, port == 993);
	}

	public RemoteFilter(String rules, String hostname, String user,
			String password, boolean ssl) throws RemoteFilterException {
		this(rules, hostname, (ssl ? 993 : 143), user, password);
	}

	private String rules;

	public RemoteFilter(String rulesSource, String hostname, int port,
			String user, String password, boolean ssl)
			throws RemoteFilterException {
		this.setDaemon(true);

		store = getRemoteStore(hostname, port, user, password, ssl);
		rules = rulesSource;
	}

	private IMAPStore getRemoteStore(String hostname, int port, String user,
			String password, boolean ssl) throws RemoteFilterException {
		Session session = Session.getDefaultInstance(new Properties());
		try {
			IMAPStore store = (IMAPStore) session.getStore((ssl ? "imaps"
					: "imap"));
			store.connect(hostname, port, user, password);
			return store;
		} catch (NoSuchProviderException e) {
			throw new RemoteFilterException(e);
		} catch (MessagingException e) {
			throw new RemoteFilterException(e);
		}
	}

	public void processMessage(Message m) {

		try {
			System.out.println();
			System.out.println("Message: " + m.getSubject());
			System.out.println("From: " + m.getFrom()[0]);
			
			Binding bindings = new Binding();		
			bindings.setVariable("m", m);
			GroovyShell groovy = new GroovyShell(bindings);
			
			groovy.evaluate(rules);
			
			String target = (String) groovy.getVariable("target");
		

			if (target != null) {
				System.out.println("Moving to " + target);
				// BEGIN TRANSACTION
				IMAPFolder f = ((IMAPFolder) store.getFolder(target));
				if (!f.exists()) {
					System.out.println("Creating " + target);
					f.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
				}
				m.getFolder().copyMessages(new Message[] { m }, f);
				// Is there a way to verify the message was copied in order to
				// fake transactions?
				m.setFlag(Flags.Flag.DELETED, true);
				Folder ff = m.getFolder();

				ff.expunge();
				// COMMIT
				// ff.open(Folder.READ_WRITE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private IMAPFolder inbox;

	private boolean threadRunning = true;

	public boolean processUnread = false;
	
	public boolean processRead = false;
	
	public void run() {
		assert (store instanceof IMAPStore);
		try {
			inbox = (IMAPFolder) store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);
		} catch (MessagingException e) {
			throw new FatalRemoteFilterError(e);
		}

		if (processUnread) {
			// Process unread messages already in the folder.
			try {
				Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
				for (Message m : unreadMessages) {
					processMessage(m);
				}
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}

		if (processRead) {
			// Process unread messages already in the folder.
			try {
				Message[] readMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), true));
				for (Message m : readMessages) {
					processMessage(m);
				}
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}

		inbox.addMessageCountListener(new MessageCountListener() {
			public void messagesAdded(MessageCountEvent messageCountEvent) {
				Message[] messages = messageCountEvent.getMessages();
				for (Message m : messages) {
					processMessage(m);
				}
			}

			public void messagesRemoved(MessageCountEvent messageCountEvent) {
				// Intentionally left empty.
			}
		});

		while (threadRunning) {
			try {
				inbox.idle();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}
	}

	private static class Prompt {
		String prompt;
		String propertyName;
		boolean secret;

		public Prompt(String prompt) {
			this(prompt, prompt.toLowerCase().replaceAll(" ", "_"));
		}

		public Prompt(String prompt, String propertyName) {
			this(prompt, propertyName, false);
		}

		public Prompt(String prompt, String propertyName, boolean secret) {
			this.prompt = prompt;
			this.propertyName = propertyName;
			this.secret = secret;
		}
	}

	private static SpringLayout.Constraints getConstraintsForCell(int row,
			int col, Container parent, int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component c = parent.getComponent(row * cols + col);
		return layout.getConstraints(c);
	}

	/**
	 * Aligns the first <code>rows</code> * <code>cols</code> components of
	 * <code>parent</code> in a grid. Each component in a column is as wide as
	 * the maximum preferred width of the components in that column; height is
	 * similarly determined for each row. The parent is made just big enough to
	 * fit them all.
	 * 
	 * @param rows
	 *            number of rows
	 * @param cols
	 *            number of columns
	 * @param initialX
	 *            x location to start the grid at
	 * @param initialY
	 *            y location to start the grid at
	 * @param xPad
	 *            x padding between cells
	 * @param yPad
	 *            y padding between cells
	 */
	public static void makeCompactGrid(Container parent, int rows, int cols,
			int initialX, int initialY, int xPad, int yPad) {
		SpringLayout layout = (SpringLayout) parent.getLayout();

		// Align all cells in each column and make them the same width.
		Spring x = Spring.constant(initialX);
		for (int c = 0; c < cols; c++) {
			Spring width = Spring.constant(0);
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width, getConstraintsForCell(r, c, parent,
						cols).getWidth());
			}
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r,
						c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
		}

		// Align all cells in each row and make them the same height.
		Spring y = Spring.constant(initialY);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height, getConstraintsForCell(r, c, parent,
						cols).getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints = getConstraintsForCell(r,
						c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
		}

		// Set the parent's size.
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);
	}

	public static Properties updateSettings(Properties properties) {
		Properties rules = new Properties();

		JDialog dialog = new JDialog();
		dialog.setModal(true);
		dialog.setLayout(new SpringLayout());

		Prompt[] prompts = new Prompt[] { new Prompt("Hostname"),
				new Prompt("Port"), new Prompt("Username"),
				new Prompt("Password", "passowrd", true) };

		for (Prompt p : prompts) {
			JLabel label = new JLabel(p.prompt + ": ", JLabel.TRAILING);
			dialog.add(label);
			JTextField textField;

			if (p.secret) {
				textField = new JPasswordField("");
			} else {
				textField = new JTextField(properties.getProperty(
						p.propertyName, ""));
			}

			label.setLabelFor(textField);
			dialog.add(textField);
		}

		dialog.add(new JButton("pipis"));

		makeCompactGrid(dialog.getContentPane(), prompts.length, 2, 4, 4, 4, 4);
		dialog.setBounds(0, 0, 250, prompts.length * 44);
		dialog.setVisible(true);
		return properties;
	}

	private static KeyStore.PrivateKeyEntry generateMyCert() throws IOException {
		try {
			Calendar calendar = Calendar.getInstance();
			Date startDate = new Date();
			calendar.setTime(startDate);
			calendar.roll(Calendar.YEAR, true);
			Date expiryDate = calendar.getTime();

			System.out.println("Generating new RSA1024 keypair...");

			Security.addProvider(new BouncyCastleProvider());
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(1024);
			KeyPair keyPair = keyGen.generateKeyPair();

			X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
			X500Principal dnName = new X500Principal(
					"CN=com.episparq.remotemailfilter."
							+ InetAddress.getLocalHost().getHostName());

			certGen.setSerialNumber(BigInteger.valueOf(System
					.currentTimeMillis()));
			certGen.setIssuerDN(dnName);
			certGen.setNotBefore(startDate);
			certGen.setNotAfter(expiryDate);
			certGen.setSubjectDN(dnName); // note: same as issuer
			certGen.setPublicKey(keyPair.getPublic());
			certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

			X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
			System.out
					.println("generating new self-signed certificate that certifies the keypair");

			KeyStore.PrivateKeyEntry e = new KeyStore.PrivateKeyEntry(keyPair
					.getPrivate(), new Certificate[] { cert });
			return e;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String fetchPasswordOrAsk() throws IOException {
		// http://stackoverflow.com/questions/727812/storing-username-password-on-mac-using-java
		String password = null;
		KeyStore.PrivateKeyEntry key = null;

		try {
			KeyStore keyStore = KeyStore.getInstance("KeychainStore", "Apple");
			keyStore.load(null, null);

			key = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
					"com.episparq.remotemailfilter",
					new KeyStore.PasswordProtection(" ".toCharArray()));
			if (key == null) {
				key = generateMyCert();
				keyStore.setEntry("com.episparq.remotemailfilter", key,
						new KeyStore.PasswordProtection("-".toCharArray()));
				keyStore.store(null, null);
			} else {
				System.out.println("Private key loaded: " + key.getPrivateKey().getAlgorithm());
			}

			/*
			 * After some experimentation, I was able to access
			 * private-key certificate entries in the KeychainStore. However,
			 * passwords in my Keychain did not show up (no alias was listed),
			 * and when I tried to add a KeyStore.SecretKeyEntry (which is what
			 * you'd need to hold a password) it failed with the message,
			 * "Key is not a PrivateKey". Clearly, Apple has not supported
			 * SecretKeyEntry.
			 */

			// Attempt to read the password from the file
			try {
				Cipher rsa = Cipher.getInstance("RSA");
				rsa.init(Cipher.DECRYPT_MODE, key.getPrivateKey());
				BufferedReader is = new BufferedReader(new InputStreamReader(
						new CipherInputStream(new FileInputStream(
								SECRETS_FILENAME), rsa)));
				password = is.readLine();
				is.close();
				rsa = null;
			} catch (Exception e) {
				System.out.println("couldnt read password from file "
						+ e.getMessage());
			}

			/**
			 * Cipher rsa = Cipher.getInstance("RSA");
			 * 
			 * rsa.init(Cipher.ENCRYPT_MODE, pk); OutputStream os = new
			 * CipherOutputStream( new FileOutputStream("encrypted.rsa"), rsa);
			 * 
			 * Writer out = new OutputStreamWriter(os);
			 * out.write("Hello World!!"); out.close(); os.close();
			 **/
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			// Ignore.
		}

		if (password == null) {
			System.out.print("Password? ");
			System.out.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					System.in));
			password = in.readLine();

			Cipher rsa;
			try {
				rsa = Cipher.getInstance("RSA");
				rsa.init(Cipher.ENCRYPT_MODE, key.getCertificate());
				Writer os = new OutputStreamWriter(new CipherOutputStream(
						new FileOutputStream(SECRETS_FILENAME), rsa));
				os.write(password);
				os.close();
				rsa = null;
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("couldnt store passowrd " + e.getMessage());
			}

		}
		return password;
	}

	private final static String USER_HOME = System.getProperty("user.home") + "/";
	private final static String PROPERTIES_FILENAME = USER_HOME
			+ ".remotefilter.properties";

	private final static String SECRETS_FILENAME = USER_HOME
			+ ".remotefilter.secrets";

	public static void main(String[] args) throws Exception {

	/*	if (SystemTray.isSupported()) {
			try {
				final TrayIcon trayIcon;
				SystemTray tray = SystemTray.getSystemTray();

				Image image = Toolkit.getDefaultToolkit().getImage(
						RemoteFilter.class.getResource("icon.png"));
				PopupMenu menu = new PopupMenu();
				trayIcon = new TrayIcon(image, "Remote Filter", menu);

				tray.add(trayIcon);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/
		
		Properties properties = new Properties();
		try {
			FileInputStream propertiesStream = new FileInputStream(new File(PROPERTIES_FILENAME));
			properties.load(propertiesStream);
			propertiesStream.close();
		} catch (IOException e) {
			// Unable to load properties from the file. Ask the user.
			// properties = updateSettings(properties);
			throw new RuntimeException("Could not read " + PROPERTIES_FILENAME);
		}

		String password = fetchPasswordOrAsk();

		InputStream rulesStream = new FileInputStream(new File(USER_HOME + (String) properties.get("rules")));
		
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(rulesStream, "UTF-8");
		int read;
		do {
		  read = in.read(buffer, 0, buffer.length);
		  if (read>0) {
		    out.append(buffer, 0, read);
		  }
		} while (read>=0);

		RemoteFilter filter = new RemoteFilter(
				out.toString(),
				properties.getProperty("host"),
				Integer.parseInt((String) properties.get("port")),
				properties.getProperty("username"),
				password);
		
		filter.processRead = Boolean.parseBoolean(properties.getProperty("process_read", "false"));
		filter.processUnread = Boolean.parseBoolean(properties.getProperty("process_unread", "false"));
		
		filter.run();
	}
}
