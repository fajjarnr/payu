import { Fragment } from "react";

export interface HeroTitleProps {
  rawTitle: string;
}

/**
 * Landing H1: renders ONLY the supported `<br />` line break from the i18n
 * string. Anything else is inert text — never interpreted as HTML.
 */
export function HeroTitle({ rawTitle }: HeroTitleProps) {
  return (
    <>
      {rawTitle.split(/<br\s*\/?>/i).map((line, index) => (
        <Fragment key={`${index}-${line}`}>
          {index > 0 && <br />}
          {line}
        </Fragment>
      ))}
    </>
  );
}
