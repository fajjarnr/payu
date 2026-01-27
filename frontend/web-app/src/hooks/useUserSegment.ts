import { useQuery, useQueryClient } from '@tanstack/react-query';
import SegmentationService from '@/services/SegmentationService';

export const useUserSegment = (userId: string | undefined) => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['user-segments', userId],
    queryFn: () => SegmentationService.getUserSegments(userId!),
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });

  const invalidateSegments = () => {
    queryClient.invalidateQueries({ queryKey: ['user-segments', userId] });
  };

  const currentMembership = query.data?.memberships.find(m => m.status === 'ACTIVE');
  const currentTier = query.data?.currentTier;
  const isVIP = currentTier ? SegmentationService.isVIPSegment(currentTier) : false;

  return {
    ...query,
    currentMembership,
    currentTier,
    isVIP,
    nextTier: query.data?.nextTier,
    progressToNext: query.data?.progressToNext,
    totalScore: query.data?.totalScore,
    invalidateSegments,
  };
};

export const useSegmentDetails = (segmentId: string | undefined) => {
  return useQuery({
    queryKey: ['segment-details', segmentId],
    queryFn: () => SegmentationService.getSegmentById(segmentId!),
    enabled: !!segmentId,
    staleTime: 10 * 60 * 1000, // 10 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
};

export const useAllSegments = () => {
  return useQuery({
    queryKey: ['all-segments'],
    queryFn: () => SegmentationService.getAllSegments(),
    staleTime: 15 * 60 * 1000, // 15 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  });
};

export default useUserSegment;
