#ifndef VIDEORENDERER_IMPL_H
#define VIDEORENDERER_IMPL_H
#include <IAgoraMediaEngine.h>
#include <QVideoFrame>
#include <QObject>
#include <QSize>
#include <mutex>

#include "frame_provider.h"

class FrameProvider;

//Instances of VideoRenderImpl is created and destroyed by Agora Media Engine.
//AVideoWidget is created and destroyed by the APP
//VideoRenderImpl must check if the video widget is alive whenever access to the view.
class VideoRenderImpl : public QObject, public agora::media::IExternalVideoRender
{
    Q_OBJECT
public:
    VideoRenderImpl(const agora::media::ExternalVideoRenerContext& context);
    ~VideoRenderImpl() override;
    virtual void release() override {delete this;}
    virtual int initialize() override {return 0;}
    virtual int deliverFrame(const agora::media::IVideoFrame& videoFrame, int rotation, bool mirrored) override;

signals:
    void recvVideoFrame(std::shared_ptr<QVideoFrame> frame, const QSize& videoSize);

public slots:
    void handleWidgetInvalidated();
    void handleViewSizeChanged(int width, int height);
private:
    FrameProvider* m_view;
    agora::media::IExternalVideoRenderCallback* m_renderCallback;
    std::mutex m_mutex;
};


#endif // VIDEORENDERER_IMPL_H
