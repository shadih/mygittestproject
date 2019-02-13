#! /bin/ksh

JAVA=/appl/javax/jdk1.7.0_25/bin/java

if [ $# -ne 1 ]
then    
       echo "NFOToUEB.sh Usage is NFOToUEB.sh start|stop|status"
       exit 1
fi

export NFOTOUEB_HOME=$HOME/current/NFOToUEB
export CLASSPATH=$CLASSPATH:\
$HOME/current/lib/*:\
$NFOTOUEB_HOME/lib/NFOToUEB.jar:\
$NFOTOUEB_HOME/etc/:\
/opt/informix/jdbc/lib/ifxjdbc.jar

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

mypid=`pgrep -f NFOTOUEB`
case $1 in

    start )
            if [ "$mypid" != "" ]
                then
                                        echo "NFOToUEB is running [$mypid]\n"
                                        exit
            fi
            echo "starting NFOToUEB"
            cd $NFOTOUEB_HOME
            $JAVA -DNFOTOUEB -Dlog_fl=$NFOTOUEB_HOME/logs/NFOToUEB.log -cp ${CLASSPATH} com.att.gfp.almv.nfotoueb.NFOToUEB &
            mypid=`pgrep -f NFOTOUEB`
            ;;
     stop )

                        if [ "$mypid" != "" ]
                        then
               echo "NFOToUEB PID is $mypid"
               echo "shutting down NFOToUEB"
               pkill -f NFOTOUEB
               sleep 1
           else
               echo "NFOToUEB not running"
           fi
           ;;
    status )
                        if [ "$mypid" != "" ]
                        then
               echo "NFOToUEB PID is $mypid"
                        else
               echo "NFOToUEB not running"
           fi
           ;;
    * )
          echo "NFOToUEB.sh Usage is NFOToUEB.sh start|stop|status"
          exit 1
     ;;
esac
