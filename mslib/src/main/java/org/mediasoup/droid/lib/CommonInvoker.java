package org.mediasoup.droid.lib;

import java.util.Collection;

/**
 * @author RainTang
 * @description:
 * @date :2022/2/14 23:00
 */
public interface CommonInvoker {
    WrapperCommon getCommonInfo(Collection<String> ids, String kind);
}
