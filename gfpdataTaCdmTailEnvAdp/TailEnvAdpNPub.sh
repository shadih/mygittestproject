#! /bin/ksh

JAVA=/appl/javax/jdk-1.7.0_51/bin/java

if [ $# -ne 1 ]
then
       echo "TailEnvAdpNPub.sh Usage is TailEnvAdpNPub.sh start|stop|status"
       exit 1
fi

#export TailEnvAdpNPub_HOME=$ROOT_DIR/TailEnvAdpNPub
export TailEnvAdpNPub_HOME=$HOME/TailEnvAdpNPub
export CLASSPATH=$CLASSPATH:\
#$ROOT_DIR/lib/highlandParkCore-0.4.3-jar-with-dependencies.jar:\
/appl/gfpd2/current/lib/highlandParkCore-0.4.3-jar-with-dependencies.jar:\
$TailEnvAdpNPub_HOME/lib/commons-io-2.4.jar:\
$TailEnvAdpNPub_HOME/lib/TailEnvAdpNPub.jar:\
$TailEnvAdpNPub_HOME/etc/


mypid=`pgrep -f CDM_TailEnvAdpNPub`

case $1 in

    start )
            if [ "$mypid" != "" ]
                then
                                        echo "TailEnvAdpNPub is running [$mypid]\n"
                                        exit
            fi
            echo "starting TailEnvAdpNPub"
            cd $TailEnvAdpNPub_HOME
            $JAVA -DCDM_TailEnvAdpNPub -Dlog_fl=$HOME/TailEnvAdpNPub/logs/TailEnvAdpNPub.log -cp ${CLASSPATH} com.att.gfp.gfpdata.gfpdataTaCdmTailEnvAdp.TailEnvAdpNPub "/appl/gfpd2/current/cienaadp/logs/Alarm.log" &
            mypid=`pgrep -f CDM_TailEnvAdpNPub`
            ;;
     stop )

                        if [ "$mypid" != "" ]
                        then
               echo "TailEnvAdpNPub PID is $mypid"
               echo "shutting down TailEnvAdpNPub"
               pkill -f CDM_TailEnvAdpNPub
               sleep 1
           else
               echo "TailEnvAdpNPub not running"
           fi
           ;;
    status )
                        if [ "$mypid" != "" ]
                        then
               echo "TailEnvAdpNPub PID is $mypid"
                        else
               echo "TailEnvAdpNPub not running"
           fi
           ;;
    * )
          echo "TailEnvAdpNPub.sh Usage is TailEnvAdpNPub.sh start|stop|status"
          exit 1
     ;;
esac