import { useQuery, useQueryClient } from '@tanstack/react-query';
import SegmentationService from '@/services/SegmentationService';

export const useSegmentedOffers = (userId: string | undefined, page = 0, size = 10) => {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['segmented-offers', userId, page, size],
    queryFn: () => SegmentationService.getSegmentedOffers(userId!, page, size),
    enabled: !!userId,
    staleTime: 2 * 60 * 1000, // 2 minutes - offers change frequently
    gcTime: 5 * 60 * 1000, // 5 minutes
  });

  const invalidateOffers = () => {
    queryClient.invalidateQueries({ queryKey: ['segmented-offers', userId] });
  };

  const activeOffers = query.data?.offers.filter(offer => offer.isActive) || [];
  const cashbackOffers = activeOffers.filter(offer => offer.offerType === 'CASHBACK');
  const discountOffers = activeOffers.filter(offer => offer.offerType === 'DISCOUNT');
  const rewardOffers = activeOffers.filter(offer => offer.offerType === 'REWARD_POINTS');
  const freeTransferOffers = activeOffers.filter(offer => offer.offerType === 'FREE_TRANSFER');

  return {
    ...query,
    offers: activeOffers,
    cashbackOffers,
    discountOffers,
    rewardOffers,
    freeTransferOffers,
    totalCount: query.data?.totalCount || 0,
    invalidateOffers,
  };
};

export const useOffersBySegment = (segmentId: string | undefined, page = 0, size = 10) => {
  return useQuery({
    queryKey: ['segment-offers', segmentId, page, size],
    queryFn: () => SegmentationService.getOffersBySegment(segmentId!, page, size),
    enabled: !!segmentId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 15 * 60 * 1000, // 15 minutes
  });
};

export const useVIPOffers = (userId: string | undefined) => {
  return useQuery({
    queryKey: ['vip-offers', userId],
    queryFn: async () => {
      const response = await SegmentationService.getSegmentedOffers(userId!, 0, 20);
      return response.offers.filter(offer =>
        offer.isActive &&
        (offer.segmentTier === 'VIP' || offer.segmentTier === 'DIAMOND' || offer.segmentTier === 'PLATINUM')
      );
    },
    enabled: !!userId,
    staleTime: 3 * 60 * 1000, // 3 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
};

export default useSegmentedOffers;
