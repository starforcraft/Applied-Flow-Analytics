package com.ultramega.appliedflowanalytics.resource;

import appeng.api.stacks.AEKey;

public record ResourceChangeGranularityKey(AEKey resourceKey, short sign, int granularity) {
}
