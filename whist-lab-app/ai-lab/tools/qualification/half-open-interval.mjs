/** Intersection of integer ranges [start, end); independent of datasets, games and storage. */
export function intervalsOverlap(left, right) {
    validate(left);
    validate(right);
    return left.start < left.end && right.start < right.end
        && left.start < right.end && right.start < left.end;
}

function validate(interval) {
    if (interval === null || typeof interval !== 'object' || Array.isArray(interval)
        || !Number.isSafeInteger(interval.start) || !Number.isSafeInteger(interval.end)) {
        throw new TypeError('Interval requires safe integer start and end');
    }
    if (interval.start > interval.end) throw new RangeError('Interval start must not exceed end');
}
