SERVICE_NAME="Fuse client"
JAR_NAME="ega-fuse-0.1.0.jar"
PATH_TO_JAR="target/$JAR_NAME"
PROCESSCNT=$(ps x |grep -v grep |grep -c "$JAR_NAME")
PID=$(ps aux | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ $PROCESSCNT == 0 ]; then
            nohup java -jar $PATH_TO_JAR $2 >> fuse-client-logs.log &
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ $PROCESSCNT != 0 ]; then
            echo "$SERVICE_NAME stopping ..."
            kill -9 $PID;
                        sleep 2s
            echo "$SERVICE_NAME stopped ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ $PROCESSCNT == 0 ]; then
            echo "$SERVICE_NAME stopping ...";
            kill -9 $PID;
                        sleep 2s
            echo "$SERVICE_NAME stopped ...";
        else
                        echo "$SERVICE_NAME is not running ..."
                fi
                echo "$SERVICE_NAME starting ..."
                nohup java -jar $PATH_TO_JAR $2  >>  fuse-client-logs.log &
                echo "$SERVICE_NAME started ..."
    ;;
esac
