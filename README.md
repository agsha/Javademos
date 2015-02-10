# A playground for all the java technologies you can name


This is a project to illustrate basic use of various technologies.
It is meant to be used as a template for your next big thing, 
or a fast way to test out how a library behaves with particular settings and input.


### Get Started

- Run a java mainClass: `mvn compile exec:java -Dexec.mainClass="hello.jacksondemo.JacksonDemo"`
- run a single testcase: `mvn -Dtest=hello.springwebdemo.HelloControllerTest test`
- Run all testcases `mvn package`
- skip running testcase: add a `-DskipTests`. e.g., `mvn package -DskipTests`
- Run jetty: `mvn jetty:run`. The context is `/helloapp`

The **BIG** awesomeness of this starter project is that *logging works!*
It was amazingly surprised how long it took me to get logging with `log4j2` exactly right.
And by exactly right, I mean:

- All ze Logging is controlled in one and only one place: `log4j2.xml`. Now you know where to enable or disable some logging
- *Everything* logs to log4j (controlled by that single `log4j2.xml`). Yes: Spring, Jetty and everything else! 
You can control *precisely* what log statements you see
- To change logging settings, *simply change `log4j2.xml` without restarting!*. The `log4j2.xml`
is polled every 5 seconds for changes

Other features of the project.

- Features a very very basic spring setup for building web applications.
- Provides an embedded jetty through maven, which monitors changes in classpath and redeploys automatically
 To use it, run 'mvn jetty:run' in one window (it will start an embedded jetty and will keep running)
 Now with every change, in another window, just do `mvn compile` (`package` or whatever) and jetty will pick up the changes automatically
- No need for even a web server! There is an example of using spring testing to fire up the
spring machinery e2e without a web server. So you can use request strings to test your code

## Pull requests welcome

- Please create a new package for the technology you are demonstrating
- Leave out all the cruft and boilerplate of java that people use in the name of software engineering. Use only the minimum code that is required to demonstrate a functionality