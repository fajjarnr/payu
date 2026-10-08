import { Flex, Spin, Typography } from 'antd';

export default function Loading() {
  return (
    <Flex align="center" justify="center" style={{ minHeight: '60vh' }}>
      <Spin tip={<Typography.Text type="secondary">Memuat data...</Typography.Text>} size="large" />
    </Flex>
  );
}
