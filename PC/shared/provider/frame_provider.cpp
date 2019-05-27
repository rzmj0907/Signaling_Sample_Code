#include "frame_provider.h"

FrameProvider::FrameProvider(QObject *parent)
    : QObject(parent)
    , m_videoFormat(QSize(0, 0), QVideoFrame::Format_YUV420P)
    , m_videoSurface(nullptr)
{
}

FrameProvider::~FrameProvider()
{
    emit providerInvalidated();
}

void FrameProvider::deliverFrame(std::shared_ptr<QVideoFrame> frame, const QSize &videoSize)
{
    if (m_videoFormat.frameSize() != videoSize) {
        // Video size changed
        m_videoSurface->stop();
        m_videoFormat.setFrameSize(videoSize);
        m_videoSurface->start(m_videoFormat);
    }

    if (m_videoSurface)
        m_videoSurface->present(*(frame.get()));
}

void FrameProvider::setVideoSurface(QAbstractVideoSurface *videoSurface)
{
    if (m_videoSurface == videoSurface)
        return;

    if (m_videoSurface && m_videoSurface->isActive())
        m_videoSurface->stop();

    m_videoSurface = videoSurface;

    if (m_videoSurface)
        m_videoSurface->start(m_videoFormat);
}
