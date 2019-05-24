var nim; // 独立通话的nim实例对象
var client; //声网的sdk的实例对象

var callingParams; // 被呼叫的时候的参数

/**
 * 初始化声网的client可以在任何时候，然后就没它什么事儿了。
 * 当呼叫双方都加入房间后，才使用声网的SDK建立音视频通话。
 * 独立信令充当了帐号系统和顶层控制指令，声网SDK相当于底层的音视频操作。
 *
 */

// 初始化声网的sdk，这个sdk可以放到登录以后，也可以在页面加载后就初始化，这里是页面加载就初始化
client = AgoraRTC.createClient({ mode: "live", codec: "h264" });
client.init(
  "**********************" /**这里传入声网的appid WRITE_APPKEY_HERE */,
  function() {
    console.log("AgoraRTC client initialized");
  },
  function(err) {
    console.log("AgoraRTC client init failed", err);
  }
);

// 一些声网的事件订阅
client.on("stream-published", function(evt) {
  console.log("Publish local stream successfully");
});
// 这个是当有人在频道里发布音视频流到频道里时
client.on("stream-added", function(evt) {
  var stream = evt.stream;
  console.log("New stream added: " + stream.getId());

  client.subscribe(stream, function(err) {
    console.log("Subscribe stream failed", err);
  });
});
// 自己订阅远端音视频流之后播放
client.on("stream-subscribed", function(evt) {
  /**
   * 在初始化流成功和订阅流成功后，都可以在回调中调用 stream.play 在页面上播放流。
   * stream.play 接受一个 dom 元素的 id 作为参数，SDK 会在这个 dom 下面创建一个 <video> 标签并播放音视频。
   */
  var remoteStream = evt.stream;
  console.log("Subscribe remote stream successfully: " + remoteStream.getId());
  // remoteStream.play("agora_remote" + remoteStream.getId());
  // 这里只有两个人通话，所以不需要定制远端播放容器，
  // 如果是多人通话，需要动态创建多个容器

  client.remoteStream = remoteStream;
  remoteStream.play("agora_remote");
});

// 当客户端因为意外掉线，而没有主动调用 signalingLeave 时，可以通过SDK的这个事件获取到，断开会话
client.on("peer-leave", function(evt) {
  var uid = evt.uid;
  console.log("remote user left ", uid);
  hangup();
});
function AgoraJoinChannel(channelId) {
  // 声网加入频道
  client.join(
    null,
    channelId,
    null,
    function(uid) {
      client.channelId = channelId;
      client.uid = uid; // 记录一下uid
      console.log("User " + uid + " join channel successfully");

      // 继续声网音视频的操作，创建音视频流
      var localStream = AgoraRTC.createStream({
        streamID: uid,
        audio: true,
        video: true,
        screen: false
      });
      // 初始化流
      localStream.init(
        function() {
          console.log("getUserMedia successfully");
          // 初始化之后播放流

          client.localStream = localStream;
          localStream.play("agora_local");
          client.publish(localStream, function(err) {
            console.log("Publish local stream error: " + err);
          });
        },
        function(err) {
          console.log("getUserMedia failed", err);
        }
      );
    },
    function(err) {
      console.log("Join channel failed", err);
    }
  );
}

// 工具函数，忽略
let steps = [
  $("#frame-1"),
  $("#frame-2"),
  $("#frame-3"),
  $("#frame-4"),
  $("#frame-5")
];
function showStep(num) {
  let index = num - 1;
  steps.forEach(function($el, $index) {
    if ($index == index) {
      $el.removeClass("hide");
    } else {
      $el.addClass("hide");
    }
  });
}

// 点击登录的操作，主要是在初始化登录nim
$("#login").on("click", function(e) {
  var account = $("#account").val();
  var token = $("#token").val();

  // 1. 这里还是nim的登录
  nim = NIM.getInstance({
    appKey: "*********************", // WRITE_APPKEY_HERE
    account: account,
    token: token,
    onconnect: function(e) {
      console.log("connected", e);
      // 登录成功后，改变界面
      showStep(2);

      // 显示用户名称，和退出登录按钮
      $(".ui-account").text(account);
    },
    ondisconnect: function(e) {
      console.log("disconnect", e);
    },
    onerror: function(e) {
      console.log("error", e);
    }
  });

  // 在线用户接收到的通知事件,登录之后
  nim.on("signalingNotify", signalingNotifyHandler);

  // 由于一些特殊情况，房间成员退出房间了的处理
  nim.on("signalingMembersSyncNotify", function(event) {
    console.log("signalingMembersSyncNotify: ", event);
    if (event.members && event.members.length == 1) {
      //如果房间里只剩一个人了，就认为对方掉线了,退出房间
      hangup();
    }
  });
});

$("#call").on("click", function() {
  let account = $("#account-call").val();
  // 这里主要是一对一的通话，背后的逻辑是创建一个channel，然后邀请用户加入这个channel。
  // 如果用户同意通话，那么双方都在nim的房间里后，可以调用声网的sdk加入到声网的一个同名的channelName中。
  var pushInfo = {
    needPush: false, //是否需要推送，默认为false
    pushTitle: "这里是推送过去的标题", //推送标题
    pushContent: "推送过去的内容", // 推送内容
    pushPayload: "", //推送自定义字段，要求是json格式，否则推送会产生错误
    needBadge: !0 ? 1 : 0 //是否计入未读计数，默认true
  };

  var param = {
    type: 2, // 通话类型,1:音频;2:视频;3:其他
    channelName: "", // 这里是房间名称，可缺省
    ext: "", //频道的自定义扩展信息，可缺省
    uid: "", //自己在频道中对应的uid，大于零有效，无效时服务器会分配随机唯一的uid， 这里不设置
    account: account, //邀请的对方account账号
    requestId: "reqId", //邀请者邀请的请求id，需要邀请者填写，之后取消邀请、拒绝、接受需要复用该requestId
    offlineEnabled: false, // 是否离线通知
    attachExt: "otherThing", // 操作者附件的自定义信息，透传给其他人
    pushInfo: pushInfo // 推送信息，具体请看nim call
  };

  // 调用主叫方调用call之后，被叫方在signalingNotifyHandler里处理邀请事件
  nim
    .signalingCall(param)
    .then(data => {
      $(".ui-account-remote").text(account);
      showStep(3);
      console.warn("独立呼叫信令，呼叫成功：", data);
    })
    .catch(error => {
      console.warn("独立呼叫信令，呼叫失败：", error);
      if (error.code == 10405) {
        console.warn("独立呼叫信令：房间已存在");
      } else if (error.code == 10201) {
        console.warn("独立呼叫信令：对方云信不在线");
      } else if (error.code == 10202) {
        console.warn("独立呼叫信令：对方推送不可达");
      }
    });
});

$("#doAcceptCall").on("click", function() {
  var channelId = callingParams.channelId;
  var account = callingParams.from; //接受谁的邀请
  var requestId = callingParams.requestId;

  var param = {
    channelId: channelId,
    account: account,
    requestId: requestId,
    offlineEnabled: false,
    attachExt: "",
    autoJoin: true, // 打开自动加入开关：该接口为组合接口，等同于先调用接受邀请，成功后再加入房间（对方会收到你接受邀请和加入房间的通知）。
    uid: "",
    joinAttachExt: "join ext"
  };

  nim
    .signalingAccept(param)
    .then(
      data => {
        console.warn("独立呼叫信令，接受别人的邀请，data：", data);
      },
      error => {
        console.warn("独立呼叫信令，接受邀请失败，error：", error);
      }
    )
    // .then(() => {
    //   return nim.signalingJoin({
    //     channelId: channelId,
    //     offlineEnabled: false,
    //     attachExt: ""
    //   });
    // })
    .then(data => {
      console.log("独立呼叫信令，接受邀请后加入房间", data);
      //先展示ui
      showStep(4);

      //调用底层的声网音视频
      AgoraJoinChannel(channelId);
    });
});

$("#doRejectCall").on("click", function() {
  var channelId = callingParams.channelId;
  var account = callingParams.from; //接受谁的邀请
  var requestId = callingParams.requestId;

  var param = {
    channelId: channelId,
    account: account,
    requestId: requestId,
    offlineEnabled: false,
    attachExt: ""
  };

  nim
    .signalingReject(param)
    .then(data => {
      showStep(2);
      console.warn("独立呼叫信令，拒绝别人的邀请，data：", data);
    })
    .catch(error => {
      console.warn("独立呼叫信令，拒绝别人的邀请失败，error：", error);
    });
});

// 本地音频关闭
$("#ui-local-audio").on("click", function() {
  var $this = $(this);
  if ($this.data("status") == "off") {
    client.localStream.enableAudio();
    $this.data("status", "on");
    this.style.fill = "white";
  } else {
    client.localStream.disableAudio();
    $this.data("status", "off");
    this.style.fill = "pink";
  }
});
// 本地视频频关闭
$("#ui-local-video").on("click", function() {
  var $this = $(this);
  if ($this.data("status") == "off") {
    client.localStream.enableVideo();
    $this.data("status", "on");
    this.style.fill = "white";
  } else {
    client.localStream.disableVideo();
    $this.data("status", "off");
    this.style.fill = "pink";
  }
});
// 远端音频关闭
$("#ui-remote-audio").on("click", function() {
  var $this = $(this);
  if ($this.data("status") == "off") {
    client.remoteStream.enableAudio();
    $this.data("status", "on");
    this.style.fill = "black";
  } else {
    client.remoteStream.disableAudio();
    $this.data("status", "off");
    this.style.fill = "#ee0000";
  }
});
// 远端视频频关闭
$("#ui-remote-video").on("click", function() {
  var $this = $(this);
  if ($this.data("status") == "off") {
    client.remoteStream.enableVideo();
    $this.data("status", "on");
    this.style.fill = "black";
  } else {
    client.remoteStream.disableVideo();
    $this.data("status", "off");
    this.style.fill = "#ee0000";
  }
});

// 挂断电话的操作
$("#hungup").on("click", hangup);
function hangup() {
  client.unpublish(client.localStream);
  client.unsubscribe(client.remoteStream);
  client.leave();
  client.localStream.stop();
  client.localStream.close();
  nim.signalingLeave({ channelId: client.channelId });
  showStep(2);
}
function logout() {
  if (nim) {
    nim = nim.destroy();
    showStep(1);
  }
}

//关闭房间
function closeRoom(channelId) {
  var param = {
    channelId: channelId,
    offlineEnabled: true,
    ext: ""
  };
  // 关闭房间操作
  nim
    .signalingClose(param)
    .then(data => {
      console.warn("独立呼叫信令，关闭房间成功，data：", data);
      alert("关闭房间成功！");
      showStep(2);
    })
    .catch(error => {
      console.warn("独立呼叫信令，关闭房间失败，error：", error);
      if (error.code == 10406) {
        console.warn("独立呼叫信令：你不在房间内，无法关闭");
      }
    });
}

function signalingNotifyHandler(event) {
  console.log("signalingOnlineNotify: ", event);
  switch (event.eventType) {
    case "ROOM_CLOSE":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
        */
      console.log("独立呼叫信令：房间关闭事件");
      alert("房间被关闭了！");
      showStep(2);

      break;
    case "ROOM_JOIN":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
          event.member 新加入的成员信息
        */
      console.log("独立呼叫信令：加入房间事件");
      //被邀请者加入房间之后，主叫方可以用声网的SDK加入房间
      showStep(4);

      //调用底层的声网音视频
      AgoraJoinChannel(event.channelId);

      break;
    case "INVITE":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
          event.toAccount 接收者的账号
          event.requestId 邀请者邀请的请求id，用于被邀请者回传request_id_作对应的回应操作
          event.pushInfo 推送信息
        */

      // 收到邀请，展示收到邀请的界面，用户选择接受还是拒绝
      callingParams = event;
      $(".ui-account-remote").text(event.from); //显示呼叫人的名字
      showStep(5);
      console.log("独立呼叫信令： 邀请事件");
      break;
    case "CANCEL_INVITE":
      /* 该事件的通知内容
      event.eventType 事件类型
      event.channelInfo 房间信息属性
      event.fromAccountId 操作者
      evnet.attachExt 操作者附加的自定义信息，透传给你
      event.time 操作的时间戳
      event.toAccount 接收者的账号
      event.requestId 邀请者邀请的请求id，用于被邀请者回传request_id_作对应的回应操作
      */

      // 呼叫被取消了，
      showStep(2);
      console.log("独立呼叫信令：取消邀请事件");
      break;
    case "REJECT":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
          event.toAccount 接收者的账号
          event.requestId 邀请者邀请的请求id，用于被邀请者回传request_id_作对应的回应操作
        */
      showStep(2);
      alert("对方拒绝");
      console.log("独立呼叫信令：拒绝邀请事件");

      break;
    case "ACCEPT":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
          event.toAccount 接收者的账号
          event.requestId 邀请者邀请的请求id，用于被邀请者回传request_id_作对应的回应操作
        */
      console.log("独立呼叫信令：接受邀请事件");
      break;
    case "LEAVE":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
        */
      console.log("独立呼叫信令：离开房间事件");

      // 挂断电话
      hangup();
      break;
    case "CONTROL":
      /* 该事件的通知内容
          event.eventType 事件类型
          event.channelInfo 房间信息属性
          event.fromAccountId 操作者
          evnet.attachExt 操作者附加的自定义信息，透传给你
          event.time 操作的时间戳
        */
      console.log("独立呼叫信令：自定义控制事件");
      break;
  }
}
