//
//  NTESP2PVC.m
//  Signalling_Agora_p2p
//
//  Created by Netease on 2019/4/26.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import "NTESP2PVC.h"
#import "NTESSignalSession.h"

@interface NTESP2PVC ()<AgoraRtcEngineDelegate>
@property (weak, nonatomic) IBOutlet UILabel *remoteIdLab;
@property (weak, nonatomic) IBOutlet UILabel *myIdLab;

@property (weak, nonatomic) IBOutlet UIView *remoteView;
@property (weak, nonatomic) IBOutlet UIView *localView;
@property (weak, nonatomic) IBOutlet UIButton *audioMuteBtn;
@property (weak, nonatomic) IBOutlet UIButton *videoMuteBtn;

@property (nonatomic, strong) NSString *agoraChannel;
@property (nonatomic, assign) NSUInteger agoraUid;
@property (nonatomic, strong) AgoraRtcEngineKit *agoraKit;
@end

@implementation NTESP2PVC

- (void)viewDidLoad {
    [super viewDidLoad];
    
    [self setupUI];
    [self initializeAgoraEngine];
    [self setupVideo];
    [self setupLocalVideo];
    [self joinMeeting];
}

- (void)setupUI {
    NSString *targetAccount = _session.accountId;
    _remoteIdLab.text = (targetAccount ?: @"");
    NSString *myAccount = [NTESDataCenter shareIntance].account;
    _myIdLab.text = (myAccount ?: @"");
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(didCloseChannel:)
                                                 name:kDidChannelCloseNotication
                                               object:nil];
}

- (IBAction)hungupAction:(id)sender {
    __weak typeof(self) weakSelf = self;
    [self leaveMeeting];
    [_session doCloseChannelCompletion:^(NSError *error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"关闭频道失败, error:%d", (int)error.code];
            [[UIApplication sharedApplication].keyWindow makeToast:msg
                                                          duration:2
                                                          position:CSToastPositionCenter];
        }
        [weakSelf.navigationController popViewControllerAnimated:YES];
    }];
}

- (IBAction)videoMuteAction:(UIButton *)sender {
    sender.selected = !sender.selected;
    _localView.hidden = sender.selected;
    [_agoraKit muteLocalVideoStream:sender.selected];
}

- (IBAction)audioMuteAction:(UIButton *)sender {
    sender.selected = !sender.selected;
    [_agoraKit muteLocalAudioStream:sender.selected];
}

- (void)didCloseChannel:(NSNotification *)note {
    //离开音视频房间
    [self leaveMeeting];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - Meeting
- (void)initializeAgoraEngine {
    _agoraKit = [AgoraRtcEngineKit sharedEngineWithAppId:NTES_DEMO_AGORA_APPID
                                                delegate:self];
}

- (void)setupVideo {
    [_agoraKit enableVideo];
    AgoraVideoEncoderConfiguration *config = [[AgoraVideoEncoderConfiguration alloc] initWithSize:AgoraVideoDimension640x360 frameRate:AgoraVideoFrameRateFps15 bitrate:AgoraVideoBitrateStandard orientationMode:AgoraVideoOutputOrientationModeAdaptative];
    [_agoraKit setVideoEncoderConfiguration:config];
}

- (void)setupLocalVideo {
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = _localView;
    canvas.renderMode = AgoraVideoRenderModeHidden;
    canvas.uid = [NTESDataCenter shareIntance].uid;
    [_agoraKit setupLocalVideo:canvas];
}

- (void)setupRemoteVideoWithUid:(NSUInteger)uid {
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = _remoteView;
    canvas.renderMode = AgoraVideoRenderModeHidden;
    canvas.uid = uid;
    [_agoraKit setupRemoteVideo:canvas];
}

- (void)joinMeeting {
    [_agoraKit setDefaultAudioRouteToSpeakerphone:YES];
    [_agoraKit setChannelProfile:AgoraChannelProfileCommunication];
    NSString *channnelId = [NTESDataCenter shareIntance].channelInfo.channelName;
    NSUInteger uid = [NTESDataCenter shareIntance].uid;

    __weak typeof(self) weakSelf = self;
    int ret = [_agoraKit joinChannelByToken:nil channelId:channnelId info:nil uid:uid joinSuccess:^(NSString * _Nonnull channel, NSUInteger uid, NSInteger elapsed) {
        weakSelf.agoraChannel = channnelId;
        weakSelf.agoraUid = uid;
    }];
    if (ret < 0) {
        NSString *msg = [NSString stringWithFormat:@"加入会议失败, error:%d", ret];
        [[UIApplication sharedApplication].keyWindow makeToast:msg
                                                      duration:2
                                                      position:CSToastPositionCenter];
    }
}

- (void)leaveMeeting {
    [_agoraKit leaveChannel:nil];
}
#pragma mark - <AgoraRtcEngineDelegate>
- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstRemoteVideoDecodedOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed {
    if (_remoteView.hidden) {
        _remoteView.hidden = NO;
    }
    [self setupRemoteVideoWithUid:uid];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didOfflineOfUid:(NSUInteger)uid reason:(AgoraUserOfflineReason)reason {
    _remoteView.hidden = YES;
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didVideoMuted:(BOOL)muted byUid:(NSUInteger)uid {
    _remoteView.hidden = muted;
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didAudioMuted:(BOOL)muted byUid:(NSUInteger)uid {
    NSString *msg = [NSString stringWithFormat:@"远端音频%@", muted ? @"关闭":@"开启"];
    [self.view makeToast:msg duration:2 position:CSToastPositionCenter];
}

@end
