
package gasstation.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name="Station", url="${external.url}")
public interface StationService {
    @RequestMapping(path="/stockFlows", method= RequestMethod.POST)
    public boolean outcome(@RequestBody StockFlow stockFlow);

}