This piece of software is designed for Mac OS X only, but will work on other platforms.
It needs Java 1.6 so you should have a recent version of Mac OS X.


For now, installation is a bit invovled and the deamon ain't pretty. You should [give me your email]("http://spreadsheets.google.com/viewform?formkey=dGg5VXYwT0IxWWctdUJfenNpdkV6WlE6MQ), or watch this project on github, so you can be notified when there's a new version.

To Install
===========

1. Open up your Terminal (use Spotlight, if needed).
2. Type: curl http://thatha.org/remotemailfilter/remotefilter.tar.gz | tar -zxf -
3. Edit remotemailfilter.properties/ If you're really into the Terminal stuff, you can use
/Applications/TextEdit.app/Contents/MacOS/TextEdit remotefilter.properties . Everything in that file should be self-explanatory.
3. Edit rules.groovy. This may look a bit complicated, but just give it a moment. Or ask me for help (AIM: thatha7777 , email: thatha@thatha.org). If "target" is set to something at the end of the script, your message will be moved to that folder. If that folder doesn't exist it will be created. If target is "null", nothing will happen. Your message is called "m". For example, the following example moves messages that contain the word "foo" in their subject to a folder called "bar":

	if (m.getSubject().contains("foo")) {
		target = "foo";
	} else {
		target = null;
	}

4. In your terminal type:
	mv remotemailfilter.properties ~/.remotemailfilter.properties
	mv rules.groovy ~/rules.groovy

5. Execute the deamon by typing
	java -jar remotemailfilter.jar

6. The first time it will ask for your password. It will encrypt it and store it in a _very_ safe way. Safer than Thunderbird, equivalent to Apple Mail.

7. It will print messages as it processes mail. If you change your rules, you need to restart the deamon. Kill it by hitting Ctrl-C, and restart it by typing "java -jar remotemailfilter.jar".

8. If you close your Terminal, the deamon will be killed. It will be come prettier (and terminal-less) in the next version.
