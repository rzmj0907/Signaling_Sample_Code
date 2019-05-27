#include "main.h"
#include "nim_cpp_client.h"
#include "login_manager.h"
#include "channel_manager.h"
#include "agorartcengine.h"
#include "frame_provider.h"

#include <QDebug>

const std::string LoginManager::kAppKey = "";
const std::string AgoraRtcEngine::kAppId = "";

int main(int argc, char *argv[])
{
    // Init NIM SDK before create application
    nim::Client::Init(LoginManager::kAppKey, "NIM_SAMPLES", "", nim::SDKConfig());

    QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
    QGuiApplication app(argc, argv);
    QQuickStyle::setStyle("Material");

    QUrl url(QStringLiteral("qrc:/main.qml"));
    LoginManager*   loginManager    = new LoginManager();
    ChannelManager* channelManager  = new ChannelManager();
    AgoraRtcEngine* agoraEngine     = new AgoraRtcEngine();

    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("loginManager", loginManager);
    engine.rootContext()->setContextProperty("channelManager", channelManager);
    engine.rootContext()->setContextProperty("agoraRtcEngine", agoraEngine);

    qmlRegisterType<FrameProvider>("NetEase.RTC.FrameProvider", 1, 0, "FrameProvider");

    QObject::connect(&engine, &QQmlApplicationEngine::objectCreated, &app, [url](QObject *obj, const QUrl &objUrl) {
        if (!obj && url == objUrl) QCoreApplication::exit(-1);
    }, Qt::QueuedConnection);
    engine.load(url);

    int res = app.exec();

    nim::Client::Cleanup2(); // cleanup with logout

    return res;
}
