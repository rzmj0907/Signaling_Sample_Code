//
//  NTESSelectedVC.m
//  Signalling_Agora_p2p
//
//  Created by Netease on 2019/4/26.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import "NTESSelectedVC.h"
#import "NTESSignalSession.h"
#import "NTESP2PVC.h"

@interface NTESSelectedVC ()<NIMSignalManagerDelegate, NTESSignalSessionDelegate>

@property (weak, nonatomic) IBOutlet UITextField *callTargetInput;
@property (weak, nonatomic) IBOutlet UIView *CallView;
@property (weak, nonatomic) IBOutlet UILabel *calledFromLab;
@property (weak, nonatomic) IBOutlet UIView *AudienceConnectingView;
@property (weak, nonatomic) IBOutlet UILabel *callTartgetLab;
@property (weak, nonatomic) IBOutlet UIView *AnchorConnectingView;

@property (nonatomic, strong) NTESSignalSession *signalSession;

@end

@implementation NTESSelectedVC

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self.view endEditing:YES];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    _signalSession = [[NTESSignalSession alloc] init];
    _signalSession.delegate = self;
    [[NIMSDK sharedSDK].signalManager addDelegate:_signalSession];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didCloseChannel:)
                                                 name:kDidChannelCloseNotication
                                               object:nil];
}

- (IBAction)loginOutAction:(id)sender {
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].loginManager logout:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"注销失败。error:%ld", (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            [NTESDataCenter shareIntance].account = nil;
            [NTESDataCenter shareIntance].token = nil;
            [weakSelf.navigationController popViewControllerAnimated:YES];
        }
    }];
}

- (IBAction)callAction:(id)sender {
    
    NSString *targetAccount = _callTargetInput.text;
    
    if (targetAccount.length == 0) {
        [self.view makeToast:@"呼叫账号为空" duration:2 position:CSToastPositionCenter];
        return;
    }
    
    [self showAnchorConnectingWithAccount:targetAccount];

    __weak typeof(self) weakSelf = self;
    [_signalSession doCallWithAccountId:targetAccount completion:^(NSError *error) {
        if (error) {
            [weakSelf showCall];
            NSString *msg = [NSString stringWithFormat:@"呼叫失败，%d", (int)error.code];
            [self.view makeToast:msg duration:2 position:CSToastPositionCenter];
        }
    }];
}

- (IBAction)acceptAction:(id)sender {
    __weak typeof(self) weakSelf = self;
    [_signalSession doAcceptCompletion:^(NSError *error) {
        if (error) {
            [weakSelf showCall];
            NSString *msg = [NSString stringWithFormat:@"接受失败，%d", (int)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            //建立音视频通话
            [weakSelf showCall];
            [weakSelf enterMeeting];
        }
    }];
}

- (IBAction)rejectAction:(id)sender {
    __weak typeof(self) weakSelf = self;
    [_signalSession doRejectCompletion:^(NSError *error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"拒绝失败，%d", (int)error.code];
            [self.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            [weakSelf showCall];
        }
    }];
}

- (void)didCloseChannel:(NSNotification *)note {
    [self.view makeToast:@"通话结束" duration:2 position:CSToastPositionCenter];
}

#pragma mark - <NTESSignalSessionDelegate>
- (void)didReceiveInvite {
    NSString *fromAccount = _signalSession.accountId;
    [self showAudienceConnectingWithAccount:fromAccount];
}

- (void)didCancelInvite {
    [self showCall];
    [self.view makeToast:@"呼叫被取消" duration:2 position:CSToastPositionCenter];
}

- (void)didRejectInvite {
    [self showCall];
    [self.view makeToast:@"呼叫被拒绝" duration:2 position:CSToastPositionCenter];
}

- (void)didJoinChannel {
    [self showCall];
    [self enterMeeting];
}

- (void)enterMeeting {
    UIStoryboard *storyBoard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
    NTESP2PVC *vc = [storyBoard instantiateViewControllerWithIdentifier:@"NTESP2PVC"];
    vc.session = _signalSession;
    [self.navigationController pushViewController:vc animated:YES];
}

#pragma mark - Private
- (void)showAnchorConnectingWithAccount:(NSString *)account {
    [self.view endEditing:YES];
    _CallView.hidden = YES;
    _AnchorConnectingView.hidden = NO;
    _callTartgetLab.text = [NSString stringWithFormat:@"正在呼叫%@...", account];
}

- (void)showAudienceConnectingWithAccount:(NSString *)account {
    [self.view endEditing:YES];
    _CallView.hidden = YES;
    _AnchorConnectingView.hidden = YES;
    _AudienceConnectingView.hidden = NO;
    _calledFromLab.text = [NSString stringWithFormat:@"%@正在呼叫你...", account];
}

- (void)showCall {
    [self.view endEditing:YES];
    _CallView.hidden = NO;
    _AnchorConnectingView.hidden = YES;
    _AudienceConnectingView.hidden = YES;
}

@end
