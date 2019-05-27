//
//  NTESDataCenter.m
//  Signaling_Agora
//
//  Created by Netease on 2019/4/24.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import "NTESDataCenter.h"

@implementation NTESDataCenter

+ (instancetype)shareIntance {
    static id instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[NTESDataCenter alloc] init];
    });
    return instance;
}

- (NSUInteger)createRandom32BitUid {
    uint32_t max = -1;
    NSUInteger ret = arc4random_uniform(max);
    while ([self uidIsExist:ret]) {
        ret = arc4random_uniform(max);
    }
    return ret;
}

- (BOOL)uidIsExist:(NSUInteger)uid {
    __block BOOL ret = NO;
    [_channelInfo.members enumerateObjectsUsingBlock:^(NIMSignalingMemberInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if (obj.uid == uid) {
            ret = YES;
            *stop = YES;
        }
    }];
    return ret;
}

@end
