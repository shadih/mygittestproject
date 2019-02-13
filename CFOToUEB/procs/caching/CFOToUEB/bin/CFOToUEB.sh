#! /bin/ksh

JAVA=/appl/javax/jdk1.7.0_25/bin/java

if [ $# -ne 1 ]
then    
       echo "CFOToUEB.sh Usage is CFOToUEB.sh start|stop|status"
       exit 1
fi

export CFOTOUEB_HOME=$HOME/current/CFOToUEB
export CLASSPATH=$CLASSPATH:\
$HOME/current/lib/*:\
$CFOTOUEB_HOME/lib/CFOToUEB.jar:\
$CFOTOUEB_HOME/etc/

#echo $CLASSPATH

# This can only run on active VCS server in PROD env

if [ $GFP_DR_SITE -eq 1 ]
then
   echo "Can't run this module on an DR server. It must be started on PROD server."
   exit
fi

www=`/usr/sbin/mount | awk '{print $1}' | grep "/gcfp/www"`
if [ $www == "/gcfp/www" ]
then
  echo "Running $0 $* "
 else
   echo "Can't run this module on an standby server. It must be started on primary server."
   exit
fi


mypid=`pgrep -f CFOTOUEB`

case $1 in

    start )
            if [ "$mypid" != "" ]
                then
                                        echo "CFOToUEB is running [$mypid]\n"
                                        exit
            fi
            echo "starting CFOToUEB"
            cd $CFOTOUEB_HOME
            $JAVA -DCFOTOUEB -Dlog_fl=$CFOTOUEB_HOME/logs/CFOToUEB.log -cp ${CLASSPATH} com.att.gfp.almv.cfotoueb.CFOToUEB &
            mypid=`pgrep -f CFOTOUEB`
            ;;
     stop )

                        if [ "$mypid" != "" ]
                        then
               echo "CFOToUEB PID is $mypid"
               echo "shutting down CFOToUEB"
               pkill -f CFOTOUEB
               sleep 1
           else
               echo "CFOToUEB not running"
           fi
           ;;
    status )
                        if [ "$mypid" != "" ]
                        then
               echo "CFOToUEB PID is $mypid"
                        else
               echo "CFOToUEB not running"
           fi
           ;;
    * )
          echo "CFOToUEB.sh Usage is CFOToUEB.sh start|stop|status"
          exit 1
     ;;
esac
