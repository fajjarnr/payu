import { render } from '@testing-library/react';
import { Col, Row } from 'antd';
import { it, expect } from 'vitest';

it('col hiding css', () => {
  render(
    <Row wrap={false}>
      <Col xs={0} md={8}>A</Col>
      <Col xs={24} md={0}>B</Col>
    </Row>,
  );
  const css = Array.from(document.querySelectorAll('style')).map((s) => s.textContent || '').join('\n');
  const idx = (s: string) => css.indexOf(s);
  console.log('XS0@', idx('.ant-col-xs-0'), 'MD8@', idx('.ant-col-md-8'), 'XS24@', idx('.ant-col-xs-24'), 'MD0@', idx('.ant-col-md-0'));
  console.log('MD8 media?', /@media[^{]*\{\s*\.ant-col-md-8/.test(css));
  console.log('XS0 media?', /@media[^{]*\{\s*\.ant-col-xs-0/.test(css));
  console.log('MD0 media?', /@media[^{]*\{\s*\.ant-col-md-0/.test(css));
  console.log('css len', css.length);
  expect(true).toBe(true);
});
