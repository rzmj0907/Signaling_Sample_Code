import QtQuick 2.4
import QtMultimedia 5.12
import QtQuick.Controls 2.5
import NetEase.RTC.FrameProvider 1.0

Rectangle {
    id: chatroom
    property alias local: local
    property alias remote: remote
    property alias microphone: microphone
    property alias hangup: hangup
    property alias camera: camera
    property alias inviteAccount: inviteAccount
    property alias inviteButton: inviteButton
    property alias chatroom: chatroom
    anchors.fill: parent
    border.width: 1
    border.color: "#9d9d9d"

    Rectangle {
        width: 310
        height: 40
        anchors.left: parent.left
        anchors.leftMargin: 15
        anchors.top: parent.top
        anchors.topMargin: 15
        color: "#00000000"
        z: 10

        Text {
            id: inviteText
            text: "邀请用户"
            font.pixelSize: 16
            anchors.verticalCenter: parent.verticalCenter
        }

        TextField {
            id: inviteAccount
            anchors.right: inviteButton.left
            anchors.rightMargin: 10
            anchors.left: inviteText.right
            anchors.leftMargin: 15
            anchors.verticalCenter: parent.verticalCenter
            text: channelManager.callAccount
            placeholderText: "对方账户"
            onTextChanged: channelManager.callAccount = text
        }

        Button {
            id: inviteButton
            height: 35
            text: "邀请"
            anchors.verticalCenter: parent.verticalCenter
            anchors.right: parent.right
            enabled: chatroom.state !== "connected"
        }
    }

    FrameProvider {
        id: local
    }

    FrameProvider {
        id: remote
    }

    VideoOutput {
        source: local
        width: chatroom.state !== "connected" ? parent.width : parent.width / 2
        height: parent.height
        anchors.left: parent.left
        fillMode: VideoOutput.PreserveAspectCrop
        z: 1
    }

    VideoOutput {
        source: remote
        width: parent.width / 2
        height: parent.height
        anchors.right: parent.right
        fillMode: VideoOutput.PreserveAspectCrop
        z: 0
    }

    Rectangle {
        width: 450
        height: 64
        anchors.horizontalCenter: parent.horizontalCenter
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 40
        color: "#00000000"
        z: 2

        ImageButton {
            id: microphone
            width: 48
            height: 48
            anchors.left: parent.left
            anchors.verticalCenter: parent.verticalCenter
            normalImage: microphone.state !== "disabled" ? '../shared/qml/images/vchat/microphone.png' : '../shared/qml/images/vchat/microphone_disabled.png'
        }

        ImageButton {
            id: hangup
            width: 64
            height: 64
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.verticalCenter: parent.verticalCenter
            normalImage: "../shared/qml/images/vchat/hangup_normal.png"
            hoveredImage: "../shared/qml/images/vchat/hangup_hovered.png"
            pushedImage: "../shared/qml/images/vchat/hangup_pushed.png"
        }

        ImageButton {
            id: camera
            width: 48
            height: 48
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            normalImage: camera.state !== "disabled" ? "../shared/qml/images/vchat/camera.png" : "../shared/qml/images/vchat/camera_disabled.png"
        }
    }
}




/*##^## Designer {
    D{i:0;autoSize:true;height:480;width:640}
}
 ##^##*/
