#include "video_render_impl.h"
#include "frame_provider.h"
#include <QDebug>
#include <QVideoFrame>

using namespace agora::media;
Q_DECLARE_METATYPE(std::shared_ptr<QVideoFrame>)

VideoRenderImpl::VideoRenderImpl(const agora::media::ExternalVideoRenerContext& context)
    :m_view(reinterpret_cast<FrameProvider*>(context.view))
    ,m_renderCallback(context.renderCallback)
{
    qDebug() << "create video render " << this << "for view " << m_view;

    qRegisterMetaType<std::shared_ptr<QVideoFrame>>();

    // Post video frame when receive video data from agora SDK.
    connect(this, SIGNAL(recvVideoFrame(std::shared_ptr<QVideoFrame>, QSize)),
            m_view, SLOT(deliverFrame(std::shared_ptr<QVideoFrame>, QSize)));

    // Destroy renderer when video provider has been closed.
    connect(m_view, SIGNAL(providerInvalidated()),
            this, SLOT(handleWidgetInvalidated()), Qt::DirectConnection);
}

VideoRenderImpl::~VideoRenderImpl()
{
    qDebug() << "video render " << this << " destroyed";
}

void VideoRenderImpl::handleWidgetInvalidated()
{
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_view = nullptr;
    }
    if (m_renderCallback)
        m_renderCallback->onViewDestroyed();
}

void VideoRenderImpl::handleViewSizeChanged(int width, int height)
{
    if (m_renderCallback)
        m_renderCallback->onViewSizeChanged(width, height);
}

int VideoRenderImpl::deliverFrame(const agora::media::IVideoFrame& videoFrame, int /*rotation*/, bool /*mirrored*/)
{
    // std::lock_guard<std::mutex> lock(m_mutex);
    std::shared_ptr<QVideoFrame> frame;

    size_t ySize = static_cast<size_t>(videoFrame.allocated_size(IVideoFrame::Y_PLANE));
    size_t uSize = static_cast<size_t>(videoFrame.allocated_size(IVideoFrame::U_PLANE));
    size_t vSize = static_cast<size_t>(videoFrame.allocated_size(IVideoFrame::V_PLANE));

    frame.reset(new QVideoFrame(static_cast<int>(ySize + uSize + vSize),
                                QSize(videoFrame.width(), videoFrame.height()),
                                videoFrame.width(),
                                QVideoFrame::Format_YUV420P));

    // Copy custom video data to a QVideoFrame object
    if (frame->map(QAbstractVideoBuffer::WriteOnly)) {

        memcpy(frame->bits(), videoFrame.buffer(IVideoFrame::Y_PLANE), ySize);
        memcpy(frame->bits() + ySize, videoFrame.buffer(IVideoFrame::U_PLANE), uSize);
        memcpy(frame->bits() + ySize + uSize, videoFrame.buffer(IVideoFrame::V_PLANE), vSize);
        frame->setStartTime(0);
        frame->unmap();

        if (m_view)
            emit recvVideoFrame(frame, QSize(videoFrame.width(), videoFrame.height()));

        return 0;
    }

    return -1;
}
