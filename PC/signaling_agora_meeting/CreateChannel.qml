import QtQuick 2.4

CreateChannelForm {
    btnLogout.onClicked: {
        loginManager.doLogout()
        pageLoader.setSource(Qt.resolvedUrl("Login.qml"))
    }

    createRoom.onClicked: {
        channelManager.createChannel()
    }

    accept.onClicked: {
        meetingForm.state = ""
        channelManager.accept()
    }

    reject.onClicked: {
        meetingForm.state = ""
        channelManager.reject()
    }

    Connections {
        target: channelManager
        onChannelNotifySignal: {
            if (eventType === 3) {
                meetingForm.state = "receivedInvitation"
                notice.text = userId + " - 邀请你加入房间."
            }
        }

        onAcceptSignal: {
            // 此时已经在信令通道中
            mainWindow.joinAgoraRoom()
            console.log("On accept signal was called.")
        }

        onCreateChannelSignal: {
            if (resCode === 200) {
                channelManager.joinChannel()
            } else {
                console.log("Failed to create channel, error code:" + resCode)
            }
        }

        onJoinChannelSignal: {
            if (resCode === 200) {
                mainWindow.joinAgoraRoom()
            } else {
                console.log("Failed to join channel, error code:" + resCode)
            }
        }
    }
}
