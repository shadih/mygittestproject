/*******************************************************************************
 *
 *  @(#) Makefile 123.1.1.1 15/12/21 15:02:07 15/12/21 15:07:52
 *
 *******************************************************************************/

/*******************************************************************************
 *
 *  Define software package locations and parameters
 *
 *******************************************************************************/

.MAIN : .all

/*******************************************************************************
 *
 *  Include system global rules
 *
 *******************************************************************************/

include "$(VROOT)/global.mk"

/*******************************************************************************
 *
 *  Define shells, source files, binaries, and libraries
 *
 *******************************************************************************/

CLASSPATH = $(VPATH:C|:| |g:X=procs/maintSrvr/lib/cambriaClient-1.1.1.jar:T=F:C| |:|g):$(VPATH:C|:| |g:X=procs/caching/CFOToUEB/etc:C| |:|g):$(VROOT)/procs/maintSrvr/lib/slf4j-log4j12-1.7.2.jar:$(VROOT)/procs/maintSrvr/lib/slf4j-api-1.7.2.jar:$(VROOT)/procs/maintSrvr/lib/log4j-1.2.17.jar:$(VROOT)/procs/maintSrvr/lib/json-20131018.jar

/*******************************************************************************
 *
 *  Define targets
 *
 *******************************************************************************/

.all : CFOToUEB.jar

:INSTALL: .all

.SOURCE.java : $(VROOT)/procs/caching/CFOToUEB/src/main/java/com/att/gfp/almv/cfotoueb

JAVA_FILES : .FUNCTIONAL .MAKE .FORCE .REPEAT .SPECIAL
	return $(*.SOURCE.java:L:M=\.java)

$(JAVA_FILES:B:S=.class) : $(JAVA_FILES) .JOINT
	$(JAVAC) -cp $(CLASSPATH) -d $(PWD) $(*:M!=\.class)

CFOToUEB.jar : $(JAVA_FILES:B:S=.class)
	$(JAR) cvf ../lib/$(<) com/att/gfp/almv/cfotoueb/*.class

/*******************************************************************************
 *
 *  Define local cleanup targets
 *
 *******************************************************************************/

CLEAN.TEMPS : .USE .NULL
