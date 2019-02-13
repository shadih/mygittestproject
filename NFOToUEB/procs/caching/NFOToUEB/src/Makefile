/*******************************************************************************
 *
 *  %Z% %M% %I% %E% %U% %D% %T%
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

CLASSPATH = $(VPATH:C|:| |g:X=procs/maintSrvr/lib/cambriaClient-1.1.1.jar:T=F:C| |:|g):$(VPATH:C|:| |g:X=procs/caching/NFOToUEB/etc:C| |:|g):$(VROOT)/procs/maintSrvr/lib/slf4j-log4j12-1.7.2.jar:$(VROOT)/procs/maintSrvr/lib/slf4j-api-1.7.2.jar:$(VROOT)/procs/maintSrvr/lib/log4j-1.2.17.jar:$(VROOT)/procs/maintSrvr/lib/json-20131018.jar

/*******************************************************************************
 *
 *  Define targets
 *
 *******************************************************************************/

.all : NFOToUEB.jar

:INSTALL: .all

.SOURCE.java : $(VROOT)/procs/caching/NFOToUEB/src/main/java/com/att/gfp/almv/nfotoueb

JAVA_FILES : .FUNCTIONAL .MAKE .FORCE .REPEAT .SPECIAL
	return $(*.SOURCE.java:L:M=\.java)

$(JAVA_FILES:B:S=.class) : $(JAVA_FILES) .JOINT
	$(JAVAC) -cp $(CLASSPATH) -d $(PWD) $(*:M!=\.class)

NFOToUEB.jar : $(JAVA_FILES:B:S=.class)
	$(JAR) cvf ../lib/$(<) com/att/gfp/almv/nfotoueb/*.class

/*******************************************************************************
 *
 *  Define local cleanup targets
 *
 *******************************************************************************/

CLEAN.TEMPS : .USE .NULL
