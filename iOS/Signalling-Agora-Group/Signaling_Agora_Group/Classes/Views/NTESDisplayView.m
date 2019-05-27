//
//  NTESDisplayView.m
//  Signalling_Agora_Group
//
//  Created by Netease on 2019/5/13.
//  Copyright © 2019 Netease. All rights reserved.
//

#import "NTESDisplayView.h"

#define MAX_DISPLAY_COUNT (5)

typedef NS_ENUM (NSInteger, NTESDisplayLayout){
    NTESDisplayLayoutSignal = 1,
    NTESDisplayLayoutTwo = 2,
    NTESDisplayLayoutThree = 3,
    NTESDisplayLayoutFour = 4,
    NTESDisplayLayoutFive = 5,
};

@interface NTESDisplayView ()
{
    CGRect _preRect;
}
@property (nonatomic, strong) NSMutableArray <NTESDisplayModel *>*models;
@property (nonatomic, strong) NSMutableArray <NSMutableArray *>*layouts;
@end

@implementation NTESDisplayView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self doCommonInit];
    }
    return self;
}

- (instancetype)initWithCoder:(NSCoder *)aDecoder {
    if (self = [super initWithCoder:aDecoder]) {
        [self doCommonInit];
    }
    return self;
}

- (void)doCommonInit {
    _models = [NSMutableArray array];
    _layouts = [NSMutableArray array];
    [self calculateLayout];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (!CGRectEqualToRect(_preRect, self.bounds)) {
        [self calculateLayout];
        [self reloadModels];
        _preRect = self.bounds;
    }
}

- (void)calculateLayout {
    [_layouts removeAllObjects];
    for (int i = 1; i <= MAX_DISPLAY_COUNT; i++) {
        switch (i) {
            case NTESDisplayLayoutSignal: {
                NSMutableArray *frames = [self layoutSignal];
                [_layouts addObject:frames];
                break;
            }
            case NTESDisplayLayoutTwo:{
                NSMutableArray *frames = [self layoutTwo];
                [_layouts addObject:frames];
                break;
            }
            case NTESDisplayLayoutThree: {
                NSMutableArray *frames = [self layoutThree];
                [_layouts addObject:frames];
                break;
            }
            case NTESDisplayLayoutFour:{
                NSMutableArray *frames = [self layoutFour];
                [_layouts addObject:frames];
                break;
            }
            case NTESDisplayLayoutFive: {
                NSMutableArray *frames = [self layoutFive];
                [_layouts addObject:frames];
                 break;
            }
            default:
                break;
        }
    }
}

- (void)reloadModels {
    [self.subviews makeObjectsPerformSelector:@selector(removeFromSuperview)];
    NSInteger layoutIndex = _models.count - 1;
    if (layoutIndex >= 0 && layoutIndex < _layouts.count) {
        NSMutableArray *frames = _layouts[layoutIndex];
        __weak typeof(self) weakSelf = self;
        [_models enumerateObjectsUsingBlock:^(NTESDisplayModel * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            obj.view.frame = [frames[idx] CGRectValue];
            obj.info.frame = obj.view.frame;
            if (idx == 0) {
                obj.info.text = [NSString stringWithFormat:@"我的账号:\n%@", obj.account];
            } else {
                obj.info.text = [NSString stringWithFormat:@"%@\n正在通话中", obj.account];
            }
            [weakSelf addSubview:obj.view];
            [weakSelf addSubview:obj.info];
        }];
    }
}

- (NSMutableArray *)layoutSignal {
    NSMutableArray *frames = [NSMutableArray array];
    CGRect frame = self.bounds;
    [frames addObject:[NSValue valueWithCGRect:frame]];
    return frames;
}

- (NSMutableArray *)layoutTwo {
    NSMutableArray *frames = [NSMutableArray array];
    for (int j = 0; j < NTESDisplayLayoutTwo; j++) {
        CGFloat w, h, x, y;
        if (self.bounds.size.width < self.bounds.size.height) {
            w = self.bounds.size.width;
            h = self.bounds.size.height/2;
            x = 0;
            y = j*h;
        } else {
            w = self.bounds.size.width/2;
            h = self.bounds.size.height;
            x = j*w;
            y = 0;
        }
        CGRect frame = CGRectMake(x, y, w, h);
        [frames addObject:[NSValue valueWithCGRect:frame]];
    }
    return frames;
}

- (NSMutableArray *)layoutThree {
    NSMutableArray *frames = [NSMutableArray array];
    for (int j = 0; j < NTESDisplayLayoutThree; j++) {
        CGFloat w, h, x, y;
        if (j == 0 || j == 1 ) {
            w = self.bounds.size.width/2;
            h = self.bounds.size.height/2;
            x = j*w;
            y = 0;
        } else {
            w = self.bounds.size.width;
            h = self.bounds.size.height/2;
            x = 0;
            y = h;
        }
        CGRect frame = CGRectMake(x, y, w, h);
        [frames addObject:[NSValue valueWithCGRect:frame]];
    }
    return frames;
}

- (NSMutableArray *)layoutFour {
    NSMutableArray *frames = [NSMutableArray array];
    for (int j = 0; j < NTESDisplayLayoutFour; j++) {
        CGFloat w, h, x, y;
        w = self.bounds.size.width/2;
        h = self.bounds.size.height/2;
        x = j%2 * w;
        y = j/2 * h;
        CGRect frame = CGRectMake(x, y, w, h);
        [frames addObject:[NSValue valueWithCGRect:frame]];
    }
    return frames;
}

- (NSMutableArray *)layoutFive {
    NSMutableArray *frames = [NSMutableArray array];
    for (int j = 0; j < NTESDisplayLayoutFive; j++) {
        CGFloat w, h, x, y;
        if (self.bounds.size.width < self.bounds.size.height) {
            if (j != NTESDisplayLayoutFive - 1) {
                w = self.bounds.size.width/2;
                h = self.bounds.size.height/3;
                x = j%2 * w;
                y = j/2 * h;
            } else {
                w = self.bounds.size.width;
                h = self.bounds.size.height/3;
                x = 0;
                y = j/2 * h;
            }
        } else {
            if (j != NTESDisplayLayoutFive - 1) {
                w = self.bounds.size.width/3;
                h = self.bounds.size.height/2;
                x = j/2 * w;
                y = j%2 * h;
            } else {
                w = self.bounds.size.width/3;
                h = self.bounds.size.height;
                x = j/2 * w;
                y = 0;
            }
        }
        CGRect frame = CGRectMake(x, y, w, h);
        [frames addObject:[NSValue valueWithCGRect:frame]];
    }
    return frames;
}

- (void)addDisplayModel:(NTESDisplayModel *)model {
    BOOL exist = NO;
    if (_models.count == MAX_DISPLAY_COUNT) {
        [self makeToast:@"最多添加5个显示" duration:2 position:CSToastPositionCenter];
        return;
    }
    
    for (NTESDisplayModel *obj in _models) {
        if (obj.uid == model.uid) {
            exist = YES;
            break;
        }
    }
    if (!exist) {
        [_models addObject:model];
        [self reloadModels];
    }
}

- (void)removeDisplayModeWithAccount:(NSString *)account {
    BOOL exist = NO;
    for (NTESDisplayModel *obj in _models) {
        if ([obj.account isEqualToString:account]) {
            exist = YES;
            [_models removeObject:obj];
            break;
        }
    }
    if (exist) {
        [self reloadModels];
    }
}

- (void)setDisplayModelHidden:(BOOL)hidden uid:(NSUInteger)uid {
    for (NTESDisplayModel *obj in _models) {
        if (obj.uid == uid) {
            obj.view.hidden = hidden;
            break;
        }
    }
}

- (BOOL)modelIsExist:(NTESDisplayModel *)model {
    BOOL exist = NO;
    for (NTESDisplayModel *obj in _models) {
        if (obj.uid == model.uid) {
            exist = YES;
            break;
        }
    }
    return exist;
}

@end


@implementation NTESDisplayModel

- (instancetype)init {
    if (self = [super init]) {
        _view = [[UIView alloc] init];
        _info = [[UILabel alloc] init];
        _info.font = [UIFont systemFontOfSize:14.0];
        _info.numberOfLines = 2;
        _info.textAlignment = NSTextAlignmentCenter;
    }
    return self;
}

@end
