SERVICE_NAME="Fuse client"
PATH_TO_JAR=target/ega-fuse-0.1.0.jar
PROCESSCNT=$(ps x |grep -v grep |grep -c "ega-fuse-0.1.0.jar")
PID=$(ps aux | grep "ega-fuse-0.1.0.jar" | grep -v grep | awk '{print $2}')
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ $PROCESSCNT == 0 ]; then
            nohup java -jar target/ega-fuse-0.1.0.jar $2 >> fuse-client-logs.log &
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
                nohup java -jar target/ega-fuse-0.1.0.jar $2  >>  fuse-client-logs.log &
                echo "$SERVICE_NAME started ..."
    ;;
esac
